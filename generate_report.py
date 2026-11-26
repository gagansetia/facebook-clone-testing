#!/usr/bin/env python3
"""
Builds an HTML "Results and Conclusion" page from the REAL Maven test results.

Run from the project folder (the one with pom.xml), AFTER the tests have run:

    mvn clean test
    python3 generate_report.py

Output: test-report.html (opens in your browser automatically).

Options:
    --reports DIR   where the XML results are   (default: target/surefire-reports)
    --out FILE      output page                 (default: test-report.html)
    --title TEXT    page title                  (default: FaceConnect Test Results)
    --no-open       do not open the browser
"""
import argparse
import glob
import html
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from datetime import datetime

# "[12/125] RUNNING : TC-012 blank display name"  (printed by FacebookServiceTest)
RUN_RE = re.compile(r"\[(\d+)/(\d+)\]\s*RUNNING\s*:\s*(.+)")
TC_RE = re.compile(r"TC-(\d+)")

CATEGORIES = [  # (last test number, category name)
    (35, "Registration / Login"),
    (50, "Profile"),
    (70, "Friend management"),
    (100, "Posts / Reactions"),
    (125, "Messaging / Search / Notifications"),
]


def category_for(name):
    m = TC_RE.search(name)
    if not m:
        return "-"
    n = int(m.group(1))
    for last, label in CATEGORIES:
        if n <= last:
            return label
    return "-"


def short_class(name):
    return (name or "").split(".")[-1]


def parse_reports(reports_dir):
    cases = []
    files = sorted(glob.glob(os.path.join(reports_dir, "TEST-*.xml")))
    for path in files:
        root = ET.parse(path).getroot()
        suites = [root] if root.tag == "testsuite" else list(root.iter("testsuite"))
        for suite in suites:
            # Map dynamic test index -> real name using the live RUNNING lines.
            index_to_name = {}
            for el in suite.iter("system-out"):
                for m in RUN_RE.finditer(el.text or ""):
                    index_to_name[int(m.group(1))] = m.group(3).strip()

            for tc in suite.findall("testcase"):
                name = tc.get("name", "")
                if not name.startswith("TC-"):
                    m = re.search(r"(\d+)\]?\s*$", name)
                    if m and int(m.group(1)) in index_to_name:
                        name = index_to_name[int(m.group(1))]

                status, detail = "passed", ""
                for tag, label in (("failure", "failed"), ("error", "failed"), ("skipped", "skipped")):
                    el = tc.find(tag)
                    if el is not None:
                        status = label
                        detail = (el.get("message") or (el.text or "")).strip()[:400]
                        break

                try:
                    secs = float(tc.get("time", "0") or 0)
                except ValueError:
                    secs = 0.0

                cases.append({
                    "name": name,
                    "class": short_class(tc.get("classname") or suite.get("name")),
                    "category": category_for(name),
                    "time": secs,
                    "status": status,
                    "detail": detail,
                })
    return cases, files


PAGE = """<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>%%TITLE%%</title>
<style>
  *{box-sizing:border-box} body{margin:0;font-family:-apple-system,"Segoe UI",Roboto,Arial,sans-serif;color:#1c1e21;background:#fff}
  .wrap{max-width:1100px;margin:0 auto;padding:28px 20px 60px}
  h1{font-size:34px;margin:0 0 20px}
  .banner{background:#0b57b7;color:#fff;border-radius:6px;padding:22px 16px 26px;text-align:center}
  .banner h2{margin:0 0 18px;font-size:24px}
  .stats{display:flex;justify-content:center;flex-wrap:wrap}
  .stat{flex:1 1 140px;max-width:260px;padding:6px 10px;border-left:1px solid rgba(255,255,255,.3)}
  .stat:first-child{border-left:0}
  .stat .l{font-size:15px;opacity:.9} .stat .v{font-size:44px;font-weight:700;line-height:1.2}
  .summary{margin:18px 0 2px;font-size:16px} .stamp{color:#8a8d91;font-size:13px}
  .chart{margin:34px auto 10px;max-width:760px}
  .legend{text-align:center;font-size:14px;color:#555;margin-bottom:6px}
  .bars{display:flex;align-items:flex-end;justify-content:space-around;height:240px;border-bottom:2px solid #333;padding:0 10px}
  .bcol{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:flex-end;height:100%}
  .bnum{font-size:14px;margin-bottom:4px} .bar{width:62%;border-radius:3px 3px 0 0;min-height:2px}
  .labels{display:flex;justify-content:space-around;padding:6px 10px 0;font-size:14px;color:#555}
  .labels div{flex:1;text-align:center}
  h3{margin:34px 0 10px;font-size:20px}
  .tools{display:flex;gap:8px;flex-wrap:wrap;align-items:center;margin-bottom:10px}
  .tools button{border:1px solid #c9ccd1;background:#f5f6f7;border-radius:18px;padding:6px 14px;cursor:pointer;font-size:14px}
  .tools button.on{background:#0b57b7;border-color:#0b57b7;color:#fff}
  .tools input{flex:1;min-width:180px;padding:7px 12px;border:1px solid #c9ccd1;border-radius:18px;font-size:14px}
  .tblwrap{overflow-x:auto;border:1px solid #dddfe2;border-radius:6px}
  table{border-collapse:collapse;width:100%;font-size:14px} th,td{padding:9px 12px;text-align:left;border-bottom:1px solid #eee;vertical-align:top}
  th{background:#f0f2f5;position:sticky;top:0} tr:hover td{background:#fafbfc}
  .badge{display:inline-block;padding:2px 10px;border-radius:12px;font-size:12px;font-weight:700;color:#fff}
  .passed{background:#1e8e3e} .failed{background:#c5221f} .skipped{background:#7a7f87}
  .detail{color:#c5221f;font-size:12px;margin-top:4px;white-space:pre-wrap}
  .muted{color:#8a8d91}
</style></head><body><div class="wrap">
<h1>Results and Conclusion</h1>
<div class="banner"><h2>Execution Results</h2>
  <div class="stats">
    <div class="stat"><div class="l">Total Tests</div><div class="v">%%TOTAL%%</div></div>
    <div class="stat"><div class="l">Passed</div><div class="v">%%PASSED%%</div></div>
    <div class="stat"><div class="l">Failed</div><div class="v">%%FAILED%%</div></div>
    <div class="stat"><div class="l">Pass Rate</div><div class="v">%%RATE%%%</div></div>
  </div></div>
<p class="summary">%%SUMMARY%%</p>
<div class="stamp">Generated %%STAMP%% &middot; %%TITLE%%</div>

<div class="chart"><div class="legend">&#9632; Execution Results</div>
  <div class="bars">
    <div class="bcol"><div class="bnum">%%TOTAL%%</div><div class="bar" style="height:%%H_TOTAL%%%;background:#14142b"></div></div>
    <div class="bcol"><div class="bnum">%%PASSED%%</div><div class="bar" style="height:%%H_PASSED%%%;background:#1e8e3e"></div></div>
    <div class="bcol"><div class="bnum">%%FAILED%%</div><div class="bar" style="height:%%H_FAILED%%%;background:#c5221f"></div></div>
    <div class="bcol"><div class="bnum">%%SKIPPED%%</div><div class="bar" style="height:%%H_SKIPPED%%%;background:#7a7f87"></div></div>
  </div>
  <div class="labels"><div>Total Tests</div><div>Passed Tests</div><div>Failed Tests</div><div>Skipped Tests</div></div>
</div>

<h3>Individual case results (%%TOTAL%% cases)</h3>
<div class="tools">
  <button class="on" data-f="all">All</button><button data-f="passed">Passed</button>
  <button data-f="failed">Failed</button><button data-f="skipped">Skipped</button>
  <input id="q" placeholder="Search test case...">
</div>
<div class="tblwrap"><table id="t"><thead><tr><th>#</th><th>Test case</th><th>Category</th><th>Class</th><th>Time (s)</th><th>Status</th></tr></thead>
<tbody>
%%ROWS%%
</tbody></table></div>
</div>
<script>
  var f="all", q=document.getElementById("q"), rows=document.querySelectorAll("#t tbody tr");
  function apply(){var s=q.value.toLowerCase();rows.forEach(function(r){
    var ok=(f==="all"||r.dataset.s===f)&&r.textContent.toLowerCase().indexOf(s)>-1;r.style.display=ok?"":"none";});}
  document.querySelectorAll(".tools button").forEach(function(b){b.onclick=function(){
    document.querySelectorAll(".tools button").forEach(function(x){x.classList.remove("on")});
    b.classList.add("on");f=b.dataset.f;apply();};});
  q.oninput=apply;
</script></body></html>
"""


def build_html(cases, title):
    total = len(cases)
    passed = sum(c["status"] == "passed" for c in cases)
    failed = sum(c["status"] == "failed" for c in cases)
    skipped = sum(c["status"] == "skipped" for c in cases)
    rate = round(passed * 100 / total, 1) if total else 0.0

    if failed:
        tail = ", review the failed cases below."
    else:
        tail = ", no failures."
    summary = f"{passed} cases passed, {failed} failed, and {skipped} were skipped{tail}"

    def h(n):
        return 0 if not total or n == 0 else max(1.0, round(n * 100 / total, 1))

    rows = []
    for i, c in enumerate(cases, 1):
        detail = f'<div class="detail">{html.escape(c["detail"])}</div>' if c["detail"] else ""
        rows.append(
            f'<tr data-s="{c["status"]}"><td class="muted">{i}</td>'
            f'<td>{html.escape(c["name"])}{detail}</td>'
            f'<td>{html.escape(c["category"])}</td><td>{html.escape(c["class"])}</td>'
            f'<td>{c["time"]:.3f}</td>'
            f'<td><span class="badge {c["status"]}">{c["status"].upper()}</span></td></tr>'
        )

    values = {
        "TITLE": html.escape(title), "TOTAL": total, "PASSED": passed, "FAILED": failed,
        "SKIPPED": skipped, "RATE": rate, "SUMMARY": html.escape(summary),
        "STAMP": datetime.now().strftime("%d %b %Y, %H:%M:%S"),
        "H_TOTAL": h(total), "H_PASSED": h(passed), "H_FAILED": h(failed), "H_SKIPPED": h(skipped),
        "ROWS": "\n".join(rows),
    }
    page = PAGE
    for key, val in values.items():
        page = page.replace(f"%%{key}%%", str(val))
    return page, (total, passed, failed, skipped, rate)


def open_in_browser(path):
    path = os.path.abspath(path)
    try:
        if sys.platform == "darwin":
            subprocess.run(["open", path], check=False)
        elif sys.platform.startswith("win"):
            os.startfile(path)  # type: ignore[attr-defined]
        else:
            subprocess.run(["xdg-open", path], check=False)
    except Exception:
        print("Open this file in your browser:", path)


def main():
    ap = argparse.ArgumentParser(description="Create an HTML results page from Maven Surefire reports.")
    ap.add_argument("--reports", default="target/surefire-reports")
    ap.add_argument("--out", default="test-report.html")
    ap.add_argument("--title", default="FaceConnect Test Results")
    ap.add_argument("--no-open", action="store_true")
    args = ap.parse_args()

    cases, files = parse_reports(args.reports)
    if not files:
        sys.exit(f"No TEST-*.xml files found in '{args.reports}'.\n"
                 "Run 'mvn clean test' first, from the folder that contains pom.xml.")
    if not cases:
        sys.exit("Result files were found but they contain no test cases.")

    page, (total, passed, failed, skipped, rate) = build_html(cases, args.title)
    with open(args.out, "w", encoding="utf-8") as fh:
        fh.write(page)

    print(f"Total: {total}  Passed: {passed}  Failed: {failed}  Skipped: {skipped}  Pass rate: {rate}%")
    print(f"Report written to: {os.path.abspath(args.out)}")
    if not args.no_open:
        open_in_browser(args.out)


if __name__ == "__main__":
    main()
