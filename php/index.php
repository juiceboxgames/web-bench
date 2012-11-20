<?php

require 'predis/lib/Predis.php';

// simple set and get scenario

$single_server = array(
    'host'     => '10.174.178.235',
    'port'     => 6379,
    'database' => 15
);

$redis = new Predis_Client($single_server);

$retval = $redis->get('user_data');
/*
$raw = gzuncompress($retval);
$document = json_decode($raw, true);
$document['TWIDDLE'] = mt_rand(0, 10000);
for($i = 0; $i < 100; $i++){
	for($j = 0; $j < 100; $j++){
		$k = sin($i) * tan($j);
	}
}
*/
//echo "Len: " . strlen($raw) . " new val : " . $document['TWIDDLE'];
//print_r(json_encode($document));

//$retval = $redis->set('user_data', gzcompress(json_encode($document)));
$redis->set('user_data', $retval);
echo "OK\n";
//print_r($retval);
