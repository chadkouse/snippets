#!/usr/local/bin/perl
# $Id: doplot.cgi,v 1.2 1997/12/14 21:31:40 dustin Exp $

use GIFgraph::bars;

push(@INC, "/home/monitor/lib");
require 'statlib.pl';

$maxping=0;
$minping=99999999;

@stat=stat($netmonPlotdata);

$lastmod=localtime($stat[9]);

open(IN, $netmonPlotdata);
while(<IN>)
{
    chop;

    @a=split(/:/, $_);

    @toplot=(@a[0], $a[5]+0, $a[3]+0);

    $minping=$a[5] if($a[5]<$minping);
    $maxping=$a[5] if($a[5]>$maxping);

    for $i (0..$#toplot)
    {
        push(@{$plotdata[$i]}, $toplot[$i]);
    }
}
close(IN);

$maxping=((int($maxping/25))*25)+25;
$minping=(int($minping/25))*25;

$im=new GIFgraph::bars(800, 200);

$im->set( 'x_label' => 'Hosts',
	'y1_label' => 'ping time (in ms)',
	'y2_label' => '% packet loss',
	'title' => "Ping times and Packet Loss (updated $lastmod)",
	'dclrs' => [ 'green', 'red'],
	'y1_max_value' => $maxping,
	'y1_min_value' => $minping,
	'y2_max_value' => 100,
	'y2_min_value' => 0,
	'two_axes' => 1,
);

print "Content-type: img/gif\nPragma: no-cache\n\n";

print $im->plot(\@plotdata);

exit 0;
