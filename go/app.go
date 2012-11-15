package main

import (
    "fmt"
    "net/http"
	"log"
	"redis"
	"runtime"
	"encoding/json"
	"compress/zlib"
	"bytes"
	"math"
	"io"
)

func responseHandler(w http.ResponseWriter, r *http.Request, client redis.AsyncClient) {
	/*
	spec := redis.DefaultSpec().Db(0).Host("10.174.178.235")

    clientLocal, err := redis.NewAsynchClientWithSpec(spec)
	if err != nil {
        	log.Println("Error creating client: ", err)
        	panic(err)
    }
    log.Println("Client Setup");
    defer client.Quit()
    */
	key := "user_data"
	getPromise, getPromiseCreationError := client.Get(key)
	//log.Println("Get");
	if getPromiseCreationError != nil {
		log.Println("Error creating client for worker: ", getPromiseCreationError)
		log.Println("frr: ", getPromise)
		panic(getPromiseCreationError)
    }
	getPromiseResult, getPromiseError := getPromise.Get()
	//log.Println("Promise Get");
	if getPromiseError != nil {
		//log.Println("Error creating client for worker: ", getPromiseError)
		//log.Println("frr: ", getPromiseError)
		panic(getPromiseError)
	}
	var inBuffer bytes.Buffer
	inBuffer.Write(getPromiseResult)
	var outBuffer bytes.Buffer
	zlibReader, zlibReadCreationError := zlib.NewReader(&inBuffer)
	if zlibReadCreationError != nil {
		log.Println("Failed to create reader")
		panic(zlibReadCreationError)
	}
	io.Copy(&outBuffer, zlibReader)
	//log.Println("Zlib Decomp");
	zlibReader.Close()
	decompressedString := outBuffer.String()
	var objmap interface{}
    decoderr := json.Unmarshal([]byte(decompressedString), &objmap)
    if decoderr != nil {
    	panic(decoderr);
    }
    //log.Println("JSON Unmarshal");
    k := 0.0;
   	for i := 0; i < 100; i++ {
	    		for j := 0; j < 100; j++ {
  	  				k = math.Sin(float64(i)) * math.Sin(float64(j))
    			}
	}
	k = 1.0
	//log.Println("Math Done");
    jsonEncoded, encodeError := json.Marshal(objmap)
    //log.Println("Marshal Done");
    if encodeError != nil {
    	panic(encodeError);
    }
    var b bytes.Buffer
    zlibWriter := zlib.NewWriter(&b)
    zlibWriter.Write([]byte(jsonEncoded))
    zlibWriter.Close()
	//log.Println("Compressed done");
    compressedString := b.String()

    setPromise, setPromiseCreationError := client.Set(key, []byte(compressedString))
    if setPromiseCreationError != nil {
        panic(setPromiseCreationError)
    }
    setPromiseResult, setPromiseError := setPromise.Get()
    if setPromiseError != nil {
        panic(setPromiseError)
    }

    //log.Println("Set Done");
    _ = setPromiseResult
    //log.Println("Set succeeded", setPromiseResult, k)
    fmt.Fprintf(w, "OK! %s", k);
}

func main() {
	runtime.GOMAXPROCS(16)
	spec := redis.DefaultSpec().Db(0).Host("10.174.178.235")
    client, err := redis.NewAsynchClientWithSpec(spec)
	if err != nil {
        	log.Println("Error creating client: ", err)
        	panic(err)
    }
    defer client.Quit()
    handler := func(w http.ResponseWriter, r *http.Request) {
    	responseHandler(w, r, client);
    }
	http.HandleFunc("/", handler)
 	http.ListenAndServe(":80", nil)
}

