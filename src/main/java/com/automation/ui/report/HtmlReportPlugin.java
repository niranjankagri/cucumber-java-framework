package com.automation.ui.report;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EmbedEvent;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.HookTestStep;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCase;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestRunStarted;
import io.cucumber.plugin.event.TestSourceRead;
import io.cucumber.plugin.event.TestStepFinished;
import io.cucumber.plugin.event.WriteEvent;

/**
 * Cucumber plugin that writes a single, self-contained HTML report when the
 * test run finishes.
 * <p>
 * The report shows a summary (verdict, pass-rate ring, scenario counts,
 * duration, browser) and, grouped by feature, one expandable card per
 * scenario with its tags, every Gherkin step with its status and duration,
 * messages written with {@code scenario.log(...)}, images attached with
 * {@code scenario.attach(...)} (e.g. the screenshot taken on failure) and,
 * for failures, the error and stack trace. All CSS and JavaScript are
 * inline, so the file can be opened or shared on its own.
 * <p>
 * Register it in {@code @CucumberOptions(plugin = ...)}:
 * <ul>
 * <li>{@code com.automation.ui.report.HtmlReportPlugin} writes to {@value #DEFAULT_REPORT},</li>
 * <li>{@code com.automation.ui.report.HtmlReportPlugin:path/to/report.html} writes to the given file.</li>
 * </ul>
 */
public class HtmlReportPlugin implements ConcurrentEventListener {

	// Report file used when no path is given as plugin argument
	public static final String DEFAULT_REPORT = "target/cucumber-reports/ui-test-report.html";

	// Date formats used in the header and the scenario details
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss")
			.withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	// "Feature: Name" line of a .feature file
	private static final Pattern FEATURE_LINE = Pattern.compile("(?m)^\\s*Feature:\\s*(.+?)\\s*$");
	// Quoted step parameters, e.g. "Admin"
	private static final Pattern QUOTED = Pattern.compile("\"([^\"]*)\"");

	// Circumference of the pass-rate ring (SVG circle with radius 52)
	private static final double RING_CIRCUMFERENCE = 2 * Math.PI * 52;

	// Where the report is written
	private final Path reportPath;

	// Feature name per feature file, from the sources Cucumber read
	private final Map<URI, String> featureNames = new LinkedHashMap<>();
	// Scenarios in the order they started
	private final Map<UUID, ScenarioRun> scenarios = new LinkedHashMap<>();
	// Start and end of the whole run
	private Instant runStarted = Instant.now();
	private Instant runFinished;

	/** Writes the report to {@value #DEFAULT_REPORT}. */
	public HtmlReportPlugin() {
		this(new File(DEFAULT_REPORT));
	}

	/** Writes the report to the given file (plugin argument after the colon). */
	public HtmlReportPlugin(File reportFile) {
		this.reportPath = reportFile.toPath();
	}

	/**
	 * Called by Cucumber once; subscribes to the events the report is built from.
	 *
	 * @param publisher Cucumber's event bus.
	 */
	@Override
	public void setEventPublisher(EventPublisher publisher) {
		publisher.registerHandlerFor(TestRunStarted.class, e -> runStarted = e.getInstant());
		publisher.registerHandlerFor(TestSourceRead.class, this::onSourceRead);
		publisher.registerHandlerFor(TestCaseStarted.class, this::onScenarioStarted);
		publisher.registerHandlerFor(TestStepFinished.class, this::onStepFinished);
		publisher.registerHandlerFor(WriteEvent.class, this::onWrite);
		publisher.registerHandlerFor(EmbedEvent.class, this::onEmbed);
		publisher.registerHandlerFor(TestCaseFinished.class, this::onScenarioFinished);
		publisher.registerHandlerFor(TestRunFinished.class, this::onRunFinished);
	}

	// ---------- Event handlers (synchronized: scenarios may run in parallel) ----------

	// A feature file was read: remember its "Feature:" name
	private synchronized void onSourceRead(TestSourceRead event) {
		Matcher m = FEATURE_LINE.matcher(event.getSource());
		featureNames.put(event.getUri(), m.find() ? m.group(1) : fileName(event.getUri()));
	}

	// A scenario started: start collecting its steps
	private synchronized void onScenarioStarted(TestCaseStarted event) {
		scenarios.put(event.getTestCase().getId(), new ScenarioRun(event.getTestCase(), event.getInstant()));
	}

	// A Gherkin step or hook finished: record its result
	private synchronized void onStepFinished(TestStepFinished event) {
		ScenarioRun run = scenarios.get(event.getTestCase().getId());
		Result result = event.getResult();
		if (event.getTestStep() instanceof PickleStepTestStep step) {
			run.entries.add(StepEntry.step(step.getStep().getKeyword().trim(), step.getStep().getText(), result));
		} else if (event.getTestStep() instanceof HookTestStep hook && result.getStatus() == Status.FAILED) {
			// Hooks are only shown when they break the scenario
			run.entries.add(StepEntry.step("@" + hook.getHookType().name().replace('_', ' ').toLowerCase(Locale.ROOT),
					hook.getCodeLocation(), result));
		}
	}

	// scenario.log(...) was called
	private synchronized void onWrite(WriteEvent event) {
		scenarios.get(event.getTestCase().getId()).entries.add(StepEntry.log(event.getText()));
	}

	// scenario.attach(...) was called, e.g. for the failure screenshot
	private synchronized void onEmbed(EmbedEvent event) {
		scenarios.get(event.getTestCase().getId()).entries
				.add(StepEntry.attachment(event.getName(), event.getMediaType(), event.getData()));
	}

	// A scenario finished: store its overall result and end time
	private synchronized void onScenarioFinished(TestCaseFinished event) {
		ScenarioRun run = scenarios.get(event.getTestCase().getId());
		run.result = event.getResult();
		run.finished = event.getInstant();
	}

	// The run finished: write the report file
	private synchronized void onRunFinished(TestRunFinished event) {
		runFinished = event.getInstant();
		try {
			Path parent = reportPath.toAbsolutePath().getParent();
			Files.createDirectories(parent);
			Files.writeString(reportPath, buildHtml(), StandardCharsets.UTF_8);
			System.out.println("HTML report generated : " + reportPath.toAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ---------- HTML ----------

	/**
	 * Builds the full HTML page from everything collected during the run.
	 */
	private String buildHtml() {
		List<ScenarioRun> runs = scenarios.values().stream().filter(r -> r.result != null).toList();
		int passed = count(runs, "pass");
		int failed = count(runs, "fail");
		int skipped = count(runs, "skip");
		int total = runs.size();
		long steps = runs.stream().mapToLong(r -> r.entries.stream().filter(s -> s.kind == Kind.STEP).count()).sum();
		long longest = runs.stream().mapToLong(r -> r.duration().toMillis()).max().orElse(1);

		// Group the scenarios by feature file, keeping execution order
		Map<URI, List<ScenarioRun>> byFeature = new LinkedHashMap<>();
		runs.forEach(r -> byFeature.computeIfAbsent(r.testCase.getUri(), k -> new ArrayList<>()).add(r));

		StringBuilder rows = new StringBuilder();
		int index = 0;
		for (Map.Entry<URI, List<ScenarioRun>> feature : byFeature.entrySet()) {
			List<ScenarioRun> list = feature.getValue();
			int featureFailed = count(list, "fail");
			rows.append("<section class=\"feature\"><div class=\"feature-head\"><h3><span class=\"kw\">Feature</span>")
					.append(escape(featureName(feature.getKey()))).append("</h3><small>")
					.append(escape(fileName(feature.getKey()))).append(" · ").append(plural(list.size(), "scenario"))
					.append(featureFailed > 0 ? " · <b class=\"bad\">" + featureFailed + " failed</b>" : "")
					.append("</small></div>");
			for (ScenarioRun run : list) {
				rows.append(buildScenarioCard(run, ++index, Math.max(longest, 1)));
			}
			rows.append("</section>");
		}
		if (runs.isEmpty()) {
			rows.append("<div class=\"empty\">No scenarios were run.</div>");
		}

		Instant end = runFinished == null ? Instant.now() : runFinished;
		String status = failed > 0 ? "fail" : (total == 0 || passed == 0 ? "skip" : "pass");
		String statusText = failed > 0 ? plural(failed, "scenario") + " failed"
				: total == 0 ? "No scenarios run" : passed == 0 ? "All scenarios skipped" : "All scenarios passed";

		return TEMPLATE
				.replace("{{STATUS}}", status)
				.replace("{{STATUS_TEXT}}", statusText)
				.replace("{{STARTED}}", DATE_TIME.format(runStarted))
				.replace("{{DURATION}}", formatDuration(Duration.between(runStarted, end)))
				.replace("{{FEATURES}}", plural(byFeature.size(), "feature"))
				.replace("{{STEPS}}", String.valueOf(steps))
				.replace("{{TOTAL}}", String.valueOf(total))
				.replace("{{PASSED}}", String.valueOf(passed))
				.replace("{{FAILED}}", String.valueOf(failed))
				.replace("{{SKIPPED}}", String.valueOf(skipped))
				.replace("{{PASS_RATE}}", total == 0 ? "–" : String.format(Locale.ROOT, "%.0f", passed * 100.0 / total))
				.replace("{{RING}}", buildRing(passed, failed, skipped, total))
				.replace("{{BAR_PASS}}", percent(passed, total))
				.replace("{{BAR_FAIL}}", percent(failed, total))
				.replace("{{BAR_SKIP}}", percent(skipped, total))
				.replace("{{BROWSER}}", escape(browser()))
				.replace("{{JAVA}}", escape(System.getProperty("java.version")))
				.replace("{{OS}}", escape(System.getProperty("os.name")))
				.replace("{{ROWS}}", rows.toString());
	}

	/**
	 * Builds the SVG segments of the pass-rate ring: one arc per status,
	 * drawn one after another around the circle.
	 */
	private String buildRing(int passed, int failed, int skipped, int total) {
		if (total == 0) {
			return "";
		}
		StringBuilder svg = new StringBuilder();
		double offset = 0;
		int[] counts = { passed, failed, skipped };
		String[] colors = { "var(--pass)", "var(--fail)", "var(--skip)" };
		for (int i = 0; i < counts.length; i++) {
			if (counts[i] == 0) {
				continue;
			}
			double length = RING_CIRCUMFERENCE * counts[i] / total;
			svg.append(String.format(Locale.ROOT,
					"<circle class=\"seg\" cx=\"60\" cy=\"60\" r=\"52\" stroke=\"%s\" stroke-dasharray=\"%.2f %.2f\" stroke-dashoffset=\"%.2f\"/>",
					colors[i], length, RING_CIRCUMFERENCE - length, -offset));
			offset += length;
		}
		return svg.toString();
	}

	/**
	 * Builds the expandable card for one scenario: header line plus details
	 * (meta data, tags, steps, logs, attachments, error and stack trace).
	 */
	private String buildScenarioCard(ScenarioRun run, int index, long longest) {
		TestCase testCase = run.testCase;
		String status = run.status();
		long duration = run.duration().toMillis();
		String location = fileName(testCase.getUri()) + ":" + testCase.getLocation().getLine();
		Throwable error = run.result.getError();

		int stepsPassed = 0;
		int stepsFailed = 0;
		int stepsSkipped = 0;
		StringBuilder stepList = new StringBuilder();
		for (StepEntry entry : run.entries) {
			if (entry.kind == Kind.STEP) {
				switch (statusName(entry.result.getStatus())) {
				case "pass" -> stepsPassed++;
				case "fail" -> stepsFailed++;
				default -> stepsSkipped++;
				}
			}
			stepList.append(renderEntry(entry));
		}

		StringBuilder tags = new StringBuilder();
		testCase.getTags().forEach(tag -> tags.append("<span class=\"tag\">").append(escape(tag)).append("</span>"));

		StringBuilder body = new StringBuilder();
		body.append("<div class=\"meta\">")
				.append(metaItem("Feature", featureName(testCase.getUri())))
				.append(metaItem("Location", location))
				.append(metaItem("Started", TIME.format(run.started)))
				.append(metaItem("Duration", formatDuration(run.duration())))
				.append("</div>");

		if (error != null && "fail".equals(status)) {
			body.append("<div class=\"error-box\">")
					.append("<div class=\"error-title\">").append(escape(error.getClass().getSimpleName())).append("</div>")
					.append("<pre>").append(escape(String.valueOf(error.getMessage()))).append("</pre>")
					.append("<details class=\"trace\"><summary>Show stack trace</summary><pre>").append(escape(stackTrace(error)))
					.append("</pre></details></div>");
		}

		body.append("<div class=\"steps-head\"><h4>Steps <span>").append(stepsPassed + stepsFailed + stepsSkipped).append("</span></h4>")
				.append("<div class=\"chips\">")
				.append(stepsPassed > 0 ? "<span class=\"chip ok\">✓ " + stepsPassed + " passed</span>" : "")
				.append(stepsFailed > 0 ? "<span class=\"chip bad\">✗ " + stepsFailed + " failed</span>" : "")
				.append(stepsSkipped > 0 ? "<span class=\"chip warn\">– " + stepsSkipped + " skipped</span>" : "")
				.append("</div></div>")
				.append("<ol class=\"steps\">").append(stepList).append("</ol>");

		String barWidth = String.format(Locale.ROOT, "%.1f", Math.max(duration * 100.0 / longest, 2));
		String searchText = (testCase.getName() + " " + featureName(testCase.getUri()) + " " + location + " "
				+ String.join(" ", testCase.getTags())).toLowerCase(Locale.ROOT);
		return "<details class=\"test " + status + "\" data-status=\"" + status + "\" data-name=\"" + escape(searchText) + "\""
				+ ("fail".equals(status) ? " open" : "") + " style=\"--i:" + index + "\">"
				+ "<summary>"
				+ "<span class=\"status-icon " + status + "\">" + statusIcon(status) + "</span>"
				+ "<span class=\"title\"><b>" + escape(testCase.getName()) + "</b>"
				+ "<small>" + escape(testCase.getKeyword()) + " · " + escape(location) + "</small>"
				+ (tags.length() > 0 ? "<span class=\"tags\">" + tags + "</span>" : "") + "</span>"
				+ "<span class=\"dur\"><span class=\"dur-text\">" + formatDuration(run.duration()) + "</span>"
				+ "<span class=\"dur-bar\"><i style=\"width:" + barWidth + "%\"></i></span></span>"
				+ "<span class=\"chev\" aria-hidden=\"true\"></span>"
				+ "</summary>"
				+ "<div class=\"body\">" + body + "</div>"
				+ "</details>";
	}

	/**
	 * Renders one Gherkin step, log message or attachment.
	 */
	private String renderEntry(StepEntry entry) {
		switch (entry.kind) {
		case LOG:
			return "<li class=\"step info\"><span class=\"dot\"></span>" + escape(entry.text) + "</li>";
		case ATTACHMENT:
			if (entry.mediaType != null && entry.mediaType.startsWith("image/")) {
				String src = "data:" + entry.mediaType + ";base64," + Base64.getEncoder().encodeToString(entry.data);
				return "<li class=\"step shot\"><span class=\"dot\"></span><figure><figcaption>" + escape(entry.text)
						+ "</figcaption><a href=\"" + src + "\" target=\"_blank\" rel=\"noopener\"><img src=\"" + src
						+ "\" alt=\"" + escape(entry.text) + "\" loading=\"lazy\"></a></figure></li>";
			}
			return "<li class=\"step info\"><span class=\"dot\"></span>Attachment: " + escape(entry.text)
					+ " (" + escape(entry.mediaType) + ", " + entry.data.length + " bytes)</li>";
		default:
			String status = statusName(entry.result.getStatus());
			Throwable error = entry.result.getError();
			return "<li class=\"step gherkin " + status + "\"><span class=\"tick\">" + statusIcon(status) + "</span>"
					+ "<span class=\"text\"><span class=\"kw\">" + escape(entry.keyword) + "</span> " + highlightArgs(entry.text)
					+ ("fail".equals(status) && error != null ? "<em class=\"step-error\">" + escape(firstLine(error.getMessage())) + "</em>" : "")
					+ "</span>"
					+ "<span class=\"step-dur\">" + ("pass".equals(status) || "fail".equals(status) ? formatDuration(entry.result.getDuration()) : entry.result.getStatus().name().toLowerCase(Locale.ROOT)) + "</span></li>";
		}
	}

	/**
	 * Escapes the step text and highlights quoted parameters such as "Admin".
	 */
	private String highlightArgs(String text) {
		Matcher m = QUOTED.matcher(text);
		StringBuilder out = new StringBuilder();
		int last = 0;
		while (m.find()) {
			out.append(escape(text.substring(last, m.start())))
					.append("<span class=\"arg\">\"").append(escape(m.group(1))).append("\"</span>");
			last = m.end();
		}
		return out.append(escape(text.substring(last))).toString();
	}

	// ---------- Helpers ----------

	// Feature name of a feature file, or the file name if it was not read
	private String featureName(URI uri) {
		return featureNames.getOrDefault(uri, fileName(uri));
	}

	// Last part of a feature URI, e.g. "login.feature"
	private static String fileName(URI uri) {
		String path = uri.toString();
		return path.substring(path.lastIndexOf('/') + 1);
	}

	// Browser from the framework settings, if they can be read
	private String browser() {
		String browser = System.getProperty("browser");
		try {
			Class<?> config = Class.forName("com.automation.ui.config.Config");
			Object instance = config.getMethod("get").invoke(null);
			browser = (String) config.getMethod("browser").invoke(instance);
			if ((Boolean) config.getMethod("headless").invoke(instance)) {
				browser += " (headless)";
			}
		} catch (ReflectiveOperationException | RuntimeException | ExceptionInInitializerError e) {
			// Settings not available: show the -D value, if any
		}
		return browser == null ? "–" : browser;
	}

	// One label/value box in the scenario details
	private String metaItem(String label, String value) {
		return "<div><span>" + label + "</span><b>" + escape(value) + "</b></div>";
	}

	// Symbol shown for a status: check, cross or dash
	private static String statusIcon(String status) {
		return switch (status) {
		case "pass" -> "✓";
		case "fail" -> "✗";
		default -> "–";
		};
	}

	/**
	 * Maps a Cucumber status to the CSS class / filter name used in the report.
	 */
	private static String statusName(Status status) {
		return switch (status) {
		case PASSED -> "pass";
		case FAILED, AMBIGUOUS, UNDEFINED -> "fail";
		default -> "skip";
		};
	}

	// Number of scenarios with the given status
	private static int count(List<ScenarioRun> runs, String status) {
		return (int) runs.stream().filter(r -> status.equals(r.status())).count();
	}

	// e.g. "1 scenario" or "3 scenarios"
	private static String plural(int count, String word) {
		return count + " " + word + (count == 1 ? "" : "s");
	}

	// part / total as a CSS percentage, e.g. "62.50"
	private static String percent(int part, int total) {
		return total == 0 ? "0" : String.format(Locale.ROOT, "%.2f", part * 100.0 / total);
	}

	/**
	 * Formats a duration as e.g. "850 ms", "4.21 s" or "2m 05s".
	 */
	private static String formatDuration(Duration duration) {
		long millis = duration == null ? 0 : duration.toMillis();
		if (millis < 1000) {
			return millis + " ms";
		}
		if (millis < 60_000) {
			return String.format(Locale.ROOT, "%.2f s", millis / 1000.0);
		}
		return String.format(Locale.ROOT, "%dm %02ds", millis / 60_000, (millis / 1000) % 60);
	}

	// First line of an error message, shown under the failed step
	private static String firstLine(String text) {
		if (text == null) {
			return "";
		}
		int newline = text.indexOf('\n');
		return newline < 0 ? text : text.substring(0, newline);
	}

	// Full stack trace of an exception as text
	private static String stackTrace(Throwable error) {
		StringWriter writer = new StringWriter();
		error.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}

	/**
	 * Escapes text for safe use inside HTML.
	 */
	private static String escape(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	// ---------- Collected data ----------

	/** One scenario (or Scenario Outline example) and everything it produced. */
	private static final class ScenarioRun {
		final TestCase testCase;
		final Instant started;
		final List<StepEntry> entries = new ArrayList<>();
		Instant finished;
		Result result;

		ScenarioRun(TestCase testCase, Instant started) {
			this.testCase = testCase;
			this.started = started;
		}

		// pass, fail or skip
		String status() {
			return statusName(result.getStatus());
		}

		// Time from scenario start to finish (zero if it never finished)
		Duration duration() {
			return finished == null ? Duration.ZERO : Duration.between(started, finished);
		}
	}

	// What a StepEntry holds
	private enum Kind { STEP, LOG, ATTACHMENT }

	/** A Gherkin step, a {@code scenario.log} message or a {@code scenario.attach} file. */
	private static final class StepEntry {
		Kind kind;
		String keyword;
		String text;
		Result result;
		String mediaType;
		byte[] data;

		// A Gherkin step (or failed hook) with its result
		static StepEntry step(String keyword, String text, Result result) {
			StepEntry e = new StepEntry();
			e.kind = Kind.STEP;
			e.keyword = keyword;
			e.text = text;
			e.result = result;
			return e;
		}

		// A message from scenario.log
		static StepEntry log(String text) {
			StepEntry e = new StepEntry();
			e.kind = Kind.LOG;
			e.text = text;
			return e;
		}

		// A file from scenario.attach
		static StepEntry attachment(String name, String mediaType, byte[] data) {
			StepEntry e = new StepEntry();
			e.kind = Kind.ATTACHMENT;
			e.text = name == null ? "Attachment" : name;
			e.mediaType = mediaType;
			e.data = data;
			return e;
		}
	}

	// HTML page with {{PLACEHOLDERS}} filled in by buildHtml()
	private static final String TEMPLATE = """
			<!doctype html>
			<html lang="en">
			<head>
			<meta charset="utf-8">
			<meta name="viewport" content="width=device-width, initial-scale=1">
			<title>UI Test Report</title>
			<link rel="preconnect" href="https://fonts.googleapis.com">
			<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
			<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
			<style>
			  :root {
			    --bg: #f4f6f5; --surface: #ffffff; --surface-2: #f7f9f8; --text: #13201b; --muted: #637069; --faint: #98a39d;
			    --border: #e3e8e5; --shadow: 0 1px 2px rgba(19,32,27,.04), 0 8px 24px -12px rgba(19,32,27,.12);
			    --pass: #12b76a; --pass-soft: #e7f8ef; --fail: #f04438; --fail-soft: #feeceb; --skip: #f79009; --skip-soft: #fef4e6;
			    --accent: #0e9f6e; --accent-soft: #e4f6ee; --code: #f1f4f2;
			    --hero-1: #0b2a22; --hero-2: #0f4a3a; --hero-3: #1fb57f;
			    color-scheme: light;
			  }
			  :root[data-theme="dark"] {
			    --bg: #0a0f0d; --surface: #121a17; --surface-2: #17211d; --text: #e5eee9; --muted: #8ea097; --faint: #5f6f67;
			    --border: #22302a; --shadow: 0 1px 2px rgba(0,0,0,.3), 0 12px 32px -16px rgba(0,0,0,.6);
			    --pass: #32d583; --pass-soft: #12301f; --fail: #ff6b5f; --fail-soft: #3a1817; --skip: #fdb022; --skip-soft: #382810;
			    --accent: #3ccf96; --accent-soft: #123327; --code: #0d1411;
			    --hero-1: #06140f; --hero-2: #0b3328; --hero-3: #149066;
			    color-scheme: dark;
			  }
			  @media (prefers-color-scheme: dark) {
			    :root:not([data-theme="light"]) {
			      --bg: #0a0f0d; --surface: #121a17; --surface-2: #17211d; --text: #e5eee9; --muted: #8ea097; --faint: #5f6f67;
			      --border: #22302a; --shadow: 0 1px 2px rgba(0,0,0,.3), 0 12px 32px -16px rgba(0,0,0,.6);
			      --pass: #32d583; --pass-soft: #12301f; --fail: #ff6b5f; --fail-soft: #3a1817; --skip: #fdb022; --skip-soft: #382810;
			      --accent: #3ccf96; --accent-soft: #123327; --code: #0d1411;
			      --hero-1: #06140f; --hero-2: #0b3328; --hero-3: #149066;
			      color-scheme: dark;
			    }
			  }
			  * { box-sizing: border-box; }
			  html { -webkit-text-size-adjust: 100%; }
			  body { margin: 0; background: var(--bg); color: var(--text);
			         font: 14.5px/1.55 Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
			         -webkit-font-smoothing: antialiased; }
			  code, pre, .mono { font-family: "JetBrains Mono", ui-monospace, SFMono-Regular, Consolas, monospace; }
			  .wrap { max-width: 1120px; margin: 0 auto; padding: 0 16px; }

			  /* ---------- Hero ---------- */
			  .hero { position: relative; overflow: hidden; color: #fff; padding: 36px 0 112px;
			          background: radial-gradient(1200px 400px at 85% -20%, var(--hero-3), transparent 60%),
			                      linear-gradient(135deg, var(--hero-1), var(--hero-2)); }
			  .hero::after { content: ""; position: absolute; inset: 0; opacity: .12; pointer-events: none;
			                 background-image: radial-gradient(rgba(255,255,255,.9) 1px, transparent 1px); background-size: 22px 22px;
			                 mask-image: linear-gradient(to bottom, #000, transparent 85%); -webkit-mask-image: linear-gradient(to bottom, #000, transparent 85%); }
			  .topbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 28px; position: relative; z-index: 1; }
			  .brand { display: flex; align-items: center; gap: 10px; font-weight: 600; font-size: 13px; letter-spacing: .02em; opacity: .9; }
			  .logo { width: 30px; height: 30px; border-radius: 9px; display: grid; place-items: center;
			          background: rgba(255,255,255,.14); border: 1px solid rgba(255,255,255,.22); }
			  .icon-btn { width: 36px; height: 36px; border-radius: 10px; border: 1px solid rgba(255,255,255,.22); cursor: pointer;
			              background: rgba(255,255,255,.1); color: #fff; display: grid; place-items: center; transition: background .2s; }
			  .icon-btn:hover { background: rgba(255,255,255,.2); }
			  .hero-main { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 16px; position: relative; z-index: 1; }
			  .eyebrow { text-transform: uppercase; letter-spacing: .14em; font-size: 11.5px; font-weight: 600; opacity: .7; margin-bottom: 6px; }
			  h1 { margin: 0; font-size: clamp(26px, 4vw, 38px); font-weight: 800; letter-spacing: -0.03em; line-height: 1.1; }
			  .hero-meta { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 14px; }
			  .hero-meta span { font-size: 12.5px; padding: 5px 11px; border-radius: 999px; background: rgba(255,255,255,.1);
			                    border: 1px solid rgba(255,255,255,.16); white-space: nowrap; }
			  .hero-meta b { font-weight: 600; }
			  .verdict { display: inline-flex; align-items: center; gap: 10px; padding: 10px 18px 10px 12px; border-radius: 999px;
			             font-weight: 700; font-size: 14px; background: rgba(255,255,255,.95); color: #13201b; box-shadow: 0 10px 30px -10px rgba(0,0,0,.5); }
			  .verdict i { width: 26px; height: 26px; border-radius: 50%; display: grid; place-items: center; color: #fff; font-style: normal; font-size: 14px; }
			  .verdict.pass i { background: #12b76a; } .verdict.fail i { background: #f04438; } .verdict.skip i { background: #f79009; }
			  .verdict.pass i::before { content: "✓"; } .verdict.fail i::before { content: "✗"; } .verdict.skip i::before { content: "–"; }

			  /* ---------- Summary ---------- */
			  .summary { margin-top: -84px; position: relative; z-index: 2; display: grid; grid-template-columns: 300px 1fr; gap: 16px; }
			  .card { background: var(--surface); border: 1px solid var(--border); border-radius: 18px; box-shadow: var(--shadow); }
			  .ring-card { padding: 24px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px; }
			  .ring { position: relative; width: 168px; height: 168px; }
			  .ring svg { width: 100%; height: 100%; transform: rotate(-90deg); }
			  .ring .track { fill: none; stroke: var(--code); stroke-width: 12; }
			  .ring .seg { fill: none; stroke-width: 12; stroke-linecap: butt; animation: draw 1s cubic-bezier(.2,.8,.2,1) both; }
			  @keyframes draw { from { stroke-dasharray: 0 400; } }
			  .ring-label { position: absolute; inset: 0; display: grid; place-items: center; text-align: center; }
			  .ring-label b { display: block; font-size: 40px; font-weight: 800; letter-spacing: -0.04em; line-height: 1; }
			  .ring-label b small { font-size: 18px; font-weight: 700; color: var(--muted); }
			  .ring-label span { font-size: 12px; color: var(--muted); text-transform: uppercase; letter-spacing: .1em; font-weight: 600; }
			  .legend { display: flex; gap: 16px; font-size: 12.5px; color: var(--muted); }
			  .legend span { display: inline-flex; align-items: center; gap: 6px; }
			  .legend i { width: 9px; height: 9px; border-radius: 3px; }

			  .stats-card { padding: 22px; display: flex; flex-direction: column; gap: 18px; }
			  .stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
			  .stat { position: relative; border-radius: 14px; padding: 16px; background: var(--surface-2); border: 1px solid var(--border); overflow: hidden; }
			  .stat::before { content: ""; position: absolute; left: 0; top: 0; bottom: 0; width: 3px; background: var(--muted); }
			  .stat.pass::before { background: var(--pass); } .stat.fail::before { background: var(--fail); } .stat.skip::before { background: var(--skip); }
			  .stat.total::before { background: var(--accent); }
			  .stat .label { font-size: 11.5px; text-transform: uppercase; letter-spacing: .09em; color: var(--muted); font-weight: 600; }
			  .stat .value { display: block; font-size: 32px; font-weight: 800; letter-spacing: -0.03em; line-height: 1.15; margin-top: 4px; font-variant-numeric: tabular-nums; }
			  .stat.pass .value { color: var(--pass); } .stat.fail .value { color: var(--fail); } .stat.skip .value { color: var(--skip); }
			  .bar { display: flex; height: 10px; border-radius: 999px; overflow: hidden; background: var(--code); }
			  .bar i { display: block; height: 100%; transition: width .8s; }
			  .bar .p { background: var(--pass); } .bar .f { background: var(--fail); } .bar .s { background: var(--skip); }
			  .facts { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
			  .fact { display: flex; flex-direction: column; gap: 2px; }
			  .fact span { font-size: 11.5px; color: var(--muted); text-transform: uppercase; letter-spacing: .08em; font-weight: 600; }
			  .fact b { font-size: 15px; font-weight: 600; overflow-wrap: anywhere; }

			  /* ---------- Toolbar ---------- */
			  .toolbar { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; margin: 36px 0 14px; }
			  .toolbar h2 { margin: 0 auto 0 0; font-size: 19px; font-weight: 700; letter-spacing: -0.02em; }
			  .toolbar h2 span { color: var(--muted); font-weight: 500; font-size: 15px; margin-left: 4px; }
			  .search { position: relative; }
			  .search input { width: 220px; max-width: 100%; height: 36px; padding: 0 12px 0 34px; border-radius: 10px; border: 1px solid var(--border);
			                  background: var(--surface); color: var(--text); font: inherit; font-size: 13.5px; outline: none; transition: border-color .2s, box-shadow .2s; }
			  .search input:focus { border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-soft); }
			  .search svg { position: absolute; left: 11px; top: 50%; transform: translateY(-50%); color: var(--faint); }
			  .segmented { display: inline-flex; padding: 3px; border-radius: 11px; background: var(--surface); border: 1px solid var(--border); }
			  .segmented button { border: 0; background: transparent; color: var(--muted); font: inherit; font-size: 13px; font-weight: 600;
			                      padding: 6px 12px; border-radius: 8px; cursor: pointer; display: inline-flex; align-items: center; gap: 6px; }
			  .segmented button em { font-style: normal; font-size: 11px; padding: 0 6px; border-radius: 999px; background: var(--code); color: var(--muted); }
			  .segmented button.active { background: var(--accent); color: #fff; }
			  .segmented button.active em { background: rgba(255,255,255,.22); color: #fff; }
			  .ghost { height: 36px; padding: 0 12px; border-radius: 10px; border: 1px solid var(--border); background: var(--surface);
			           color: var(--text); font: inherit; font-size: 13px; font-weight: 600; cursor: pointer; }
			  .ghost:hover, .segmented button:not(.active):hover { color: var(--text); background: var(--surface-2); }

			  /* ---------- Features ---------- */
			  .feature { margin-bottom: 26px; }
			  .feature-head { display: flex; flex-wrap: wrap; align-items: baseline; justify-content: space-between; gap: 6px 12px; margin: 0 4px 10px; }
			  .feature-head h3 { margin: 0; font-size: 16px; font-weight: 700; letter-spacing: -0.01em; }
			  .feature-head small { color: var(--muted); font-size: 12.5px; }
			  .feature-head .bad { color: var(--fail); font-weight: 700; }
			  .kw { color: var(--accent); font-weight: 700; }
			  .feature-head .kw { margin-right: 8px; font-size: 12px; text-transform: uppercase; letter-spacing: .1em; }

			  /* ---------- Scenario cards ---------- */
			  .test { background: var(--surface); border: 1px solid var(--border); border-radius: 16px; margin-bottom: 12px;
			          box-shadow: var(--shadow); overflow: hidden; animation: rise .45s cubic-bezier(.2,.8,.2,1) both;
			          animation-delay: calc(var(--i) * 40ms); }
			  @keyframes rise { from { opacity: 0; transform: translateY(8px); } }
			  .test > summary { list-style: none; cursor: pointer; display: flex; align-items: center; gap: 14px; padding: 16px 18px; transition: background .15s; }
			  .test > summary::-webkit-details-marker { display: none; }
			  .test > summary:hover { background: var(--surface-2); }
			  .status-icon { flex: none; width: 34px; height: 34px; border-radius: 10px; display: grid; place-items: center; font-weight: 800; font-size: 15px; }
			  .status-icon.pass { background: var(--pass-soft); color: var(--pass); }
			  .status-icon.fail { background: var(--fail-soft); color: var(--fail); }
			  .status-icon.skip { background: var(--skip-soft); color: var(--skip); }
			  .title { flex: 1; min-width: 0; }
			  .title b { display: block; font-weight: 600; font-size: 15px; overflow-wrap: anywhere; }
			  .title small { display: block; color: var(--muted); font-size: 12px; overflow-wrap: anywhere; }
			  .tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 6px; }
			  .tag { font: 500 11px/1 "JetBrains Mono", ui-monospace, monospace; padding: 4px 7px; border-radius: 6px; background: var(--accent-soft); color: var(--accent); }
			  .dur { display: flex; flex-direction: column; align-items: flex-end; gap: 6px; width: 120px; flex: none; }
			  .dur-text { font-size: 13px; font-weight: 600; font-variant-numeric: tabular-nums; }
			  .dur-bar { width: 100%; height: 4px; border-radius: 999px; background: var(--code); overflow: hidden; }
			  .dur-bar i { display: block; height: 100%; border-radius: 999px; background: var(--accent); }
			  .test.fail .dur-bar i { background: var(--fail); } .test.skip .dur-bar i { background: var(--skip); }
			  .chev { flex: none; width: 20px; height: 20px; border-radius: 6px; position: relative; color: var(--faint); }
			  .chev::before { content: ""; position: absolute; left: 6px; top: 5px; width: 7px; height: 7px; border-right: 2px solid currentColor;
			                  border-bottom: 2px solid currentColor; transform: rotate(45deg); transition: transform .2s; }
			  .test[open] .chev::before { transform: rotate(-135deg); top: 9px; }
			  .body { padding: 4px 18px 20px 66px; border-top: 1px solid var(--border); }
			  .meta { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px; margin: 14px 0 4px; }
			  .meta div { background: var(--surface-2); border: 1px solid var(--border); border-radius: 10px; padding: 8px 12px; min-width: 0; }
			  .meta span { display: block; font-size: 11px; text-transform: uppercase; letter-spacing: .08em; color: var(--muted); font-weight: 600; }
			  .meta b { display: block; font-size: 13px; font-weight: 600; overflow-wrap: anywhere; }

			  .error-box { margin-top: 16px; border-radius: 12px; padding: 14px 16px; background: var(--fail-soft); border: 1px solid color-mix(in srgb, var(--fail) 30%, transparent); }
			  .error-title { font-weight: 700; font-size: 13px; color: var(--fail); margin-bottom: 6px; }
			  .error-box pre { margin: 0; white-space: pre-wrap; overflow-wrap: anywhere; font-size: 12.5px; line-height: 1.6; }
			  .trace { margin-top: 10px; }
			  .trace summary { cursor: pointer; font-size: 12.5px; font-weight: 600; color: var(--accent); }
			  .trace pre { margin-top: 8px; padding: 12px; border-radius: 8px; background: var(--surface); max-height: 320px; overflow: auto; color: var(--muted); }

			  .steps-head { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 8px; margin: 20px 0 10px; }
			  h4 { margin: 0; font-size: 12px; text-transform: uppercase; letter-spacing: .1em; color: var(--muted); font-weight: 700; }
			  h4 span { margin-left: 6px; padding: 1px 8px; border-radius: 999px; background: var(--code); color: var(--text); font-size: 11px; }
			  .chips { display: flex; flex-wrap: wrap; gap: 6px; }
			  .chip { font-size: 12px; font-weight: 600; padding: 3px 10px; border-radius: 999px; background: var(--accent-soft); color: var(--accent); }
			  .chip.ok { background: var(--pass-soft); color: var(--pass); } .chip.bad { background: var(--fail-soft); color: var(--fail); }
			  .chip.warn { background: var(--skip-soft); color: var(--skip); }
			  .steps { list-style: none; margin: 0; padding: 6px; border-radius: 12px; background: var(--code); border: 1px solid var(--border); }
			  .step { display: flex; align-items: flex-start; gap: 10px; padding: 7px 10px; border-radius: 8px; font-size: 13.5px; min-width: 0; overflow-wrap: anywhere; }
			  .step:hover { background: var(--surface); }
			  .step .text { flex: 1; min-width: 0; }
			  .step .kw { margin-right: 2px; }
			  .arg { font-family: "JetBrains Mono", ui-monospace, monospace; font-size: 12.5px; color: var(--accent); }
			  .step-dur { flex: none; font-size: 12px; color: var(--faint); font-variant-numeric: tabular-nums; padding-top: 1px; }
			  .step-error { display: block; margin-top: 4px; font-style: normal; font-size: 12.5px; color: var(--fail); }
			  .tick { flex: none; width: 20px; height: 20px; border-radius: 50%; display: grid; place-items: center; font-size: 11px; font-weight: 800; }
			  .gherkin.pass .tick { background: var(--pass-soft); color: var(--pass); }
			  .gherkin.fail .tick { background: var(--fail); color: #fff; }
			  .gherkin.fail { background: var(--fail-soft); }
			  .gherkin.skip .tick { background: var(--skip-soft); color: var(--skip); }
			  .gherkin.skip .text { color: var(--muted); }
			  .dot { flex: none; width: 6px; height: 6px; margin: 7px 7px 0; border-radius: 50%; background: var(--faint); }
			  .info { color: var(--muted); font-size: 12.5px; }
			  .shot figure { margin: 0; flex: 1; min-width: 0; }
			  .shot figcaption { font-size: 12.5px; color: var(--muted); margin-bottom: 6px; }
			  .shot img { display: block; max-width: 100%; max-height: 360px; border-radius: 8px; border: 1px solid var(--border); }

			  .empty, .no-match { text-align: center; color: var(--muted); padding: 48px 16px; background: var(--surface);
			                      border: 1px dashed var(--border); border-radius: 16px; }
			  .no-match { display: none; }
			  footer { margin: 40px 0 48px; text-align: center; font-size: 12.5px; color: var(--faint); }
			  footer b { color: var(--muted); font-weight: 600; }

			  @media (max-width: 860px) {
			    .summary { grid-template-columns: 1fr; }
			    .stats, .facts { grid-template-columns: repeat(2, 1fr); }
			  }
			  @media (max-width: 560px) {
			    .hero { padding-bottom: 100px; }
			    .body { padding-left: 18px; }
			    .dur { width: 72px; }
			    .search, .search input { width: 100%; }
			    .toolbar h2 { width: 100%; }
			  }
			  @media (prefers-reduced-motion: reduce) { * { animation: none !important; transition: none !important; } }
			  @media print {
			    .icon-btn, .toolbar .search, .segmented, .ghost { display: none; }
			    .hero { padding-bottom: 32px; } .summary { margin-top: 16px; }
			    .test { break-inside: avoid; box-shadow: none; }
			  }
			</style>
			</head>
			<body>
			<section class="hero">
			  <div class="wrap">
			    <div class="topbar">
			      <div class="brand">
			        <span class="logo"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 9h18"/><path d="M8 14l2 2 4-4"/></svg></span>
			        cucumber-java-framework · Test Report
			      </div>
			      <button class="icon-btn" id="theme" title="Toggle light / dark theme" aria-label="Toggle theme">
			        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg>
			      </button>
			    </div>
			    <div class="hero-main">
			      <div>
			        <div class="eyebrow">Cucumber UI test run</div>
			        <h1>OrangeHRM UI Tests</h1>
			        <div class="hero-meta">
			          <span>Started <b>{{STARTED}}</b></span>
			          <span>Duration <b>{{DURATION}}</b></span>
			          <span><b>{{FEATURES}}</b></span>
			          <span>{{STEPS}} <b>steps</b></span>
			        </div>
			      </div>
			      <div class="verdict {{STATUS}}"><i></i>{{STATUS_TEXT}}</div>
			    </div>
			  </div>
			</section>

			<div class="wrap">
			  <section class="summary">
			    <div class="card ring-card">
			      <div class="ring">
			        <svg viewBox="0 0 120 120"><circle class="track" cx="60" cy="60" r="52"/>{{RING}}</svg>
			        <div class="ring-label"><div><b>{{PASS_RATE}}<small>%</small></b><span>Pass rate</span></div></div>
			      </div>
			      <div class="legend">
			        <span><i style="background:var(--pass)"></i>Passed</span>
			        <span><i style="background:var(--fail)"></i>Failed</span>
			        <span><i style="background:var(--skip)"></i>Skipped</span>
			      </div>
			    </div>
			    <div class="card stats-card">
			      <div class="stats">
			        <div class="stat total"><span class="label">Scenarios</span><span class="value">{{TOTAL}}</span></div>
			        <div class="stat pass"><span class="label">Passed</span><span class="value">{{PASSED}}</span></div>
			        <div class="stat fail"><span class="label">Failed</span><span class="value">{{FAILED}}</span></div>
			        <div class="stat skip"><span class="label">Skipped</span><span class="value">{{SKIPPED}}</span></div>
			      </div>
			      <div class="bar" title="Passed {{PASSED}} · Failed {{FAILED}} · Skipped {{SKIPPED}}">
			        <i class="p" style="width:{{BAR_PASS}}%"></i><i class="f" style="width:{{BAR_FAIL}}%"></i><i class="s" style="width:{{BAR_SKIP}}%"></i>
			      </div>
			      <div class="facts">
			        <div class="fact"><span>Duration</span><b>{{DURATION}}</b></div>
			        <div class="fact"><span>Browser</span><b>{{BROWSER}}</b></div>
			        <div class="fact"><span>Java</span><b>{{JAVA}}</b></div>
			        <div class="fact"><span>OS</span><b>{{OS}}</b></div>
			      </div>
			    </div>
			  </section>

			  <div class="toolbar">
			    <h2>Scenarios <span>{{TOTAL}}</span></h2>
			    <label class="search">
			      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M20 20l-3.5-3.5"/></svg>
			      <input id="search" type="search" placeholder="Search name or @tag" aria-label="Search scenarios">
			    </label>
			    <div class="segmented" role="group" aria-label="Filter by status">
			      <button class="active" data-filter="all">All <em>{{TOTAL}}</em></button>
			      <button data-filter="pass">Passed <em>{{PASSED}}</em></button>
			      <button data-filter="fail">Failed <em>{{FAILED}}</em></button>
			      <button data-filter="skip">Skipped <em>{{SKIPPED}}</em></button>
			    </div>
			    <button class="ghost" id="toggle-all">Expand all</button>
			  </div>

			  <main id="tests">{{ROWS}}<div class="no-match" id="no-match">No scenarios match the current filter.</div></main>

			  <footer>Generated by <b>HtmlReportPlugin</b> · {{STARTED}}</footer>
			</div>

			<script>
			  (function () {
			    var root = document.documentElement;

			    // Theme: remember the viewer's choice; fall back to the system theme
			    try { var saved = localStorage.getItem('report-theme'); if (saved) root.dataset.theme = saved; } catch (e) {}
			    document.getElementById('theme').addEventListener('click', function () {
			      var dark = root.dataset.theme ? root.dataset.theme === 'dark' : matchMedia('(prefers-color-scheme: dark)').matches;
			      root.dataset.theme = dark ? 'light' : 'dark';
			      try { localStorage.setItem('report-theme', root.dataset.theme); } catch (e) {}
			    });

			    // Status filter + text search, combined; features with no visible scenario are hidden
			    var tests = Array.prototype.slice.call(document.querySelectorAll('.test'));
			    var features = Array.prototype.slice.call(document.querySelectorAll('.feature'));
			    var status = 'all';
			    var search = document.getElementById('search');
			    function apply() {
			      var q = search.value.trim().toLowerCase();
			      var shown = 0;
			      tests.forEach(function (t) {
			        var ok = (status === 'all' || t.dataset.status === status) && (!q || t.dataset.name.indexOf(q) !== -1);
			        t.style.display = ok ? '' : 'none';
			        if (ok) shown++;
			      });
			      features.forEach(function (f) {
			        var any = Array.prototype.some.call(f.querySelectorAll('.test'), function (t) { return t.style.display !== 'none'; });
			        f.style.display = any ? '' : 'none';
			      });
			      document.getElementById('no-match').style.display = tests.length && !shown ? 'block' : 'none';
			    }
			    document.querySelectorAll('.segmented button').forEach(function (b) {
			      b.addEventListener('click', function () {
			        document.querySelectorAll('.segmented button').forEach(function (x) { x.classList.remove('active'); });
			        b.classList.add('active');
			        status = b.dataset.filter;
			        apply();
			      });
			    });
			    search.addEventListener('input', apply);

			    // Expand / collapse every scenario card
			    var toggle = document.getElementById('toggle-all');
			    toggle.addEventListener('click', function () {
			      var open = toggle.textContent === 'Expand all';
			      tests.forEach(function (t) { t.open = open; });
			      toggle.textContent = open ? 'Collapse all' : 'Expand all';
			    });
			  })();
			</script>
			</body>
			</html>
			""";
}
