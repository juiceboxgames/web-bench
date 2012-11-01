<?php

require 'predis/lib/Predis.php';

$document = json_decode(file_get_contents("../util/document.json"), true);

// simple set and get scenario

$single_server = array(
    'host'     => '10.174.178.235',
    'port'     => 6379,
    'database' => 15
);

$redis = new Predis_Client($single_server);

$retval = $redis->get('user_data');
$document = json_decode(gzuncompress($retval), true);
echo $document['TWIDDLE'] . "\n";
$document['TWIDDLE'] = mt_rand(0, 10000);
$retval = $redis->set('user_data', gzcompress(json_encode($document)));
print_r($retval);
