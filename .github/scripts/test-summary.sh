#!/usr/bin/env bash
set -euo pipefail

title="$1"
reports_dir="$2"
summary="${GITHUB_STEP_SUMMARY:-/dev/stdout}"

shopt -s nullglob
reports=("$reports_dir"/TEST-*.xml)

if [ ${#reports[@]} -eq 0 ]; then
  printf '## %s\n\nNo report found in `%s`.\n' "$title" "$reports_dir" >> "$summary"
  exit 0
fi

perl -ne '
  if (/<testsuite\b([^>]*)>/) {
    my $attrs = $1;
    my %suite;
    for my $key (qw(tests failures errors skipped)) {
      $suite{$key} = $attrs =~ /\b$key="(\d+)"/ ? $1 : 0;
      $total{$key} += $suite{$key};
    }
    my ($name) = $attrs =~ /\bname="([^"]+)"/;
    push @failing, $name if $suite{failures} + $suite{errors} > 0;
  }
  END {
    my $passed = $total{tests} - $total{failures} - $total{errors} - $total{skipped};
    my $status = $total{failures} + $total{errors} > 0 ? "❌" : "✅";
    print "## $status '"$title"'\n\n";
    print "| Tests | Passed | Failed | Errors | Skipped |\n";
    print "|---|---|---|---|---|\n";
    print "| $total{tests} | $passed | $total{failures} | $total{errors} | $total{skipped} |\n";
    if (@failing) {
      print "\nFailing classes:\n\n";
      print "- `$_`\n" for @failing;
    }
  }
' "${reports[@]}" >> "$summary"
