var sys = require('sys');
var zlib = require('zlib');
var cluster = require('cluster');
var restify = require('restify');
var numCPUs = require('os').cpus().length * 2;
var redis = require("./redis-client.js");
var port = 6379;
var host = "10.174.178.235";
var options = {};
if (cluster.isMaster) {
    // Fork workers.
    for (var i = 0; i < numCPUs; i++) {
        cluster.fork();
    }

    cluster.on('exit', function(worker, code, signal) {
        console.log('worker ' + worker.process.pid + ' died');
    });
} else {
    var client = redis.createClient(port, host, options);
    var server = restify.createServer();
    function respond(req, res, next) {

        /*
        client.get("user_data", function (err, val) {
            zlib.gzip(val, function (gzErr, compressed){
                   client.set("user_data", compressed, function (setResult, setTruthy){
                       res.send("Set result correctly");
                   });
            });
        });
        */

        client.get("user_data", function (err, val) {
            zlib.gunzip(val, function (gzErr, uncompressed){
                 doc = JSON.parse(uncompressed);
                 doc.TWIDDLE = Math.floor(30000 * Math.random());
                 for(i = 0; i < 100; i++){
                     for(j = 0; j < 100; j++){
                        k = Math.sin(i) * Math.tan(j);
                     }
                 }
                 jsonDoc = JSON.stringify(doc);
                 zlib.gzip(jsonDoc, function (gzErr, compressed){
                    client.set("user_data", compressed, function (setResult, setTruthy){
                        res.send("OK");
                    });
                });
            });
        });

    }
    server.get('/test', respond);
    server.listen(61337, function() {
        console.log('%s listening at %s', server.name, server.url);
    });
}