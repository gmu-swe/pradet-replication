#!/bin/bash

for f in *.jar; do
    s=`md5sum $f | cut -d ' ' -f 1`;
    p=`wget -q -O - "http://www.jarvana.com/jarvana/search?search_type=content&content=${s}&filterContent=digest" | grep inspect-pom | cut -d \" -f 4`;
    pj="http://www.jarvana.com${p}";
    rm -f tmp;
    wget -q -O tmp "$pj";

    g=`grep groupId tmp | head -n 1 | cut -d \> -f 3 | cut -d \< -f 1`;
    a=`grep artifactId tmp | head -n 1 | cut -d \> -f 3 | cut -d \< -f 1`;
    v=`grep version tmp | head -n 1 | cut -d \> -f 3 | cut -d \< -f 1`;
    rm -f tmp;

    echo '<dependency> <!--' $f $s $pj '-->';
    echo "  <groupId>$g</groupId>";
    echo "  <artifactId>$a</artifactId>";
    echo "  <version>$v</version>";
    echo "</dependency>";
    echo;
done
