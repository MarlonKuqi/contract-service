#!/usr/bin/env bash
set -euo pipefail

report="$1"
output="$2"
max_rows="${3:-30}"

if [ ! -f "$report" ]; then
  printf '## 🧬 Mutation testing (PIT)\n\nNo report found at `%s`.\n' "$report" > "$output"
  exit 0
fi

changed_lines=""
if [ -n "${DIFF_RANGE:-}" ]; then
  changed_lines="$(mktemp)"
  trap 'rm -f "$changed_lines"' EXIT
  git diff -U0 --no-color "$DIFF_RANGE" -- 'src/main/java/*.java' | perl -ne '
    if (m{^\+\+\+ b/(\S+)}) { $file = $1; next }
    if (m{^\+\+\+ }) { undef $file; next }
    if (defined $file && /^@@ -\S+ \+(\d+)(?:,(\d+))? @@/) {
      my ($start, $length) = ($1, $2 // 1);
      print "$file:$_\n" for $start .. $start + $length - 1;
    }
  ' > "$changed_lines"
fi

MAX_ROWS="$max_rows" CHANGED_LINES="$changed_lines" UNDETECTED_FILE="${BRANCH_UNDETECTED_FILE:-}" perl -0777 -ne '
  my $max = $ENV{MAX_ROWS};
  my %changed;
  my $scoped = $ENV{CHANGED_LINES} ne "";
  if ($scoped) {
    open my $fh, "<", $ENV{CHANGED_LINES} or die "Cannot read $ENV{CHANGED_LINES}: $!";
    my $content = do { local $/; <$fh> };
    $changed{$_} = 1 for split /\n/, $content;
  }

  my (%count, %branch_count, @undetected, @branch_undetected);
  while (/<mutation\b([^>]*)>(.*?)<\/mutation>/gs) {
    my ($attrs, $body) = ($1, $2);
    my ($status) = $attrs =~ /status=["\x27](\w+)["\x27]/;
    next if !defined $status || $status eq "NON_VIABLE";
    my ($detected) = $attrs =~ /detected=["\x27](\w+)["\x27]/;
    my %field;
    for my $tag (qw(sourceFile mutatedClass mutatedMethod lineNumber description)) {
      ($field{$tag}) = $body =~ /<$tag>(.*?)<\/$tag>/s;
      $field{$tag} //= "";
    }
    (my $package_path = $field{mutatedClass}) =~ s/\.[^.]*$//;
    $package_path =~ s/\./\//g;
    my $in_branch = $scoped && $changed{"src/main/java/$package_path/$field{sourceFile}:$field{lineNumber}"};

    $count{total}++;
    $count{detected}++ if $detected eq "true";
    $count{$status}++;
    if ($in_branch) {
      $branch_count{total}++;
      $branch_count{detected}++ if $detected eq "true";
    }
    next if $detected eq "true";

    (my $class = $field{mutatedClass}) =~ s/.*\.//;
    $field{description} =~ s/&lt;/</g; $field{description} =~ s/&gt;/>/g;
    $field{description} =~ s/&amp;/&/g; $field{description} =~ s/\|/\\|/g;
    my $mutant = [$status, $class, $field{mutatedMethod}, $field{lineNumber}, $field{description}];
    push @undetected, $mutant;
    push @branch_undetected, $mutant if $in_branch;
  }

  sub print_mutants {
    my $max = $ENV{MAX_ROWS};
    my @mutants = sort { $a->[1] cmp $b->[1] || $a->[3] <=> $b->[3] } @_;
    print "| Status | Class | Method | Line | Mutation |\n";
    print "|---|---|---|---|---|\n";
    my $shown = 0;
    for my $m (@mutants) {
      last if $shown++ >= $max;
      print "| $m->[0] | `$m->[1]` | `$m->[2]` | $m->[3] | $m->[4] |\n";
    }
    my $hidden = @mutants - $max;
    print "\n… and $hidden more in the `mutation-report` artifact.\n" if $hidden > 0;
  }

  my $total = $count{total} // 0;
  my $detected = $count{detected} // 0;
  my $survived = $count{SURVIVED} // 0;
  my $no_coverage = $count{NO_COVERAGE} // 0;
  my $covered = $total - $no_coverage;
  my $score = $total ? sprintf("%.0f %%", 100 * $detected / $total) : "n/a";
  my $strength = $covered ? sprintf("%.0f %%", 100 * $detected / $covered) : "n/a";
  my $status = @undetected ? "⚠️" : "✅";

  print "## $status Mutation testing (PIT)\n\n";
  print "| Mutants | Killed | Survived | No coverage | Mutation score | Test strength |\n";
  print "|---|---|---|---|---|---|\n";
  print "| $total | $detected | $survived | $no_coverage | $score | $strength |\n";

  if (@undetected) {
    print "\n<details><summary>Undetected mutants (" . scalar(@undetected) . ")</summary>\n\n";
    print_mutants(@undetected);
    print "\n</details>\n";
  }

  if ($ENV{UNDETECTED_FILE} ne "") {
    open my $out, ">", $ENV{UNDETECTED_FILE} or die "Cannot write $ENV{UNDETECTED_FILE}: $!";
    print $out scalar(@branch_undetected), "\n";
    close $out;
  }

  if ($scoped) {
    my $branch_total = $branch_count{total} // 0;
    my $branch_detected = $branch_count{detected} // 0;
    my $branch_status = @branch_undetected ? "⚠️" : "✅";
    print "\n### $branch_status Mutants on lines changed by this branch\n\n";
    if (!$branch_total) {
      print "No mutant on the lines changed by this branch.\n";
    } else {
      print "| Mutants | Killed | Undetected |\n";
      print "|---|---|---|\n";
      print "| $branch_total | $branch_detected | " . scalar(@branch_undetected) . " |\n";
      if (@branch_undetected) {
        print "\n";
        print_mutants(@branch_undetected);
      }
    }
  }
' "$report" > "$output"
