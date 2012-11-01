<?php

require '../php/predis/lib/Predis.php';

$document = json_decode(file_get_contents("document.json"), true);

// simple set and get scenario

$single_server = array(
    'host'     => '10.174.178.235',
    'port'     => 6379,
    'database' => 15
);

$redis = new Predis_Client($single_server);


$redis->set('user_data', gzcompress(json_encode($document)));
//print_r($retval);
