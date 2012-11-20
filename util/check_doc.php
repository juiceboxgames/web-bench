<?php

require '../php/predis/lib/Predis.php';


// simple set and get scenario

$single_server = array(
    'host'     => '10.174.178.235',
    'port'     => 6379,
    'database' => 0
);

$redis = new Predis_Client($single_server);



$result = $redis->get('user_data_2');
echo strlen($result) . "\n";

