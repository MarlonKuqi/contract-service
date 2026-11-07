#!/usr/bin/env bash
set -euo pipefail

report="$1"
output="$2"
max_rows="${3:-30}"

if [ ! -f "$report" ]; then
  printf '## 🧬 Mutation testing (PIT)\n\nNo report found at `%s`.\n' "$report" > "$output"
  exit 0
fi

MAX_ROWS="$max_rows" perl -0777 -ne '
  my %count;
  my @undetected;
  while (/<mutation\b([^>]*)>(.*?)<\/mutation>/gs) {
    my ($attrs, $body) = ($1, $2);
    my ($status) = $attrs =~ /status=["\x27](\w+)["\x27]/;
    next if !defined $status || $status eq "NON_VIABLE";
    my ($detected) = $attrs =~ /detected=["\x27](\w+)["\x27]/;
    $count{total}++;
    $count{detected}++ if $detected eq "true";
    $count{$status}++;
    next if $detected eq "true";
    my %field;
    for my $tag (qw(mutatedClass mutatedMethod lineNumber description)) {
      ($field{$tag}) = $body =~ /<$tag>(.*?)<\/$tag>/s;
      $field{$tag} //= "";
    }
    (my $class = $field{mutatedClass}) =~ s/.*\.//;
    $field{description} =~ s/&lt;/</g; $field{description} =~ s/&gt;/>/g;
    $field{description} =~ s/&amp;/&/g; $field{description} =~ s/\|/\\|/g;
    push @undetected, [$status, $class, $field{mutatedMethod}, $field{lineNumber}, $field{description}];
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
    my $max = $ENV{MAX_ROWS};
    @undetected = sort { $a->[1] cmp $b->[1] || $a->[3] <=> $b->[3] } @undetected;
    print "\n<details><summary>Undetected mutants (" . scalar(@undetected) . ")</summary>\n\n";
    print "| Status | Class | Method | Line | Mutation |\n";
    print "|---|---|---|---|---|\n";
    my $shown = 0;
    for my $m (@undetected) {
      last if $shown++ >= $max;
      print "| $m->[0] | `$m->[1]` | `$m->[2]` | $m->[3] | $m->[4] |\n";
    }
    my $hidden = @undetected - $max;
    print "\n… and $hidden more in the `mutation-report` artifact.\n" if $hidden > 0;
    print "\n</details>\n";
  }
' "$report" > "$output"
