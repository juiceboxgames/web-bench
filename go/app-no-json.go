package main

import (
	"bytes"
	"compress/zlib"
	//"encoding/json"
	"fmt"
	"log"
	"math"
	"net/http"
	"redis"
	"bufio"
	"os"
	//"io"
	"io/ioutil"
	"runtime"
)

type ClientPool struct {
	size int
	clientChan chan redis.AsyncClient
}

func NewClientPool(size int) *ClientPool {
	p := &ClientPool{size: size}
	p.clientChan = make(chan redis.AsyncClient, size)
	for i := 0; i < size; i++ {
		client := NewRedisAsyncClient()
		p.clientChan <- client
	}
	return p
}

func (p *ClientPool) Get() redis.AsyncClient {
	return <-p.clientChan
}

func (p *ClientPool) Release(c redis.AsyncClient) {
	p.clientChan <- c
}

var pool *ClientPool


var loadtimeout uint64 = 0
var savetimeout uint64 = 0

func load(r redis.AsyncClient, k string, w http.ResponseWriter) (obj interface{}) {
	f, rerr := r.Get(k)
	if rerr != nil {
		panic(rerr)
	}
	val, rerr, timeout := f.TryGet(50000000000)
	if rerr != nil {
		panic(rerr)
	}
	if timeout {
		loadtimeout++
		log.Println("load timeout! count: ", loadtimeout)
		fmt.Fprintf(w, "Save failed for %s", key);
		return
	}
	setPromise, srerr := r.Set(key, val)
	if srerr != nil {
		panic(rerr)
	}
	_, grerr, timeout := setPromise.TryGet(50000000000)
	if grerr != nil {
		panic(rerr)
	}
	if timeout {
		savetimeout++
		log.Println("save timeout! count: ", savetimeout)
		fmt.Fprintf(w, "Save failed for %s", key);
	}
	return;
}

func compute() {
	var k float64
	for i := 0; i < 100; i++ {
		for j := 0; j < 100; j++ {
			k = math.Sin(float64(i)) * math.Sin(float64(j))
		}
	}
	_ = k
	//log.Println("Math Done")
}


type User struct {
	Id      uint64
	Name    string
	Xp      uint64
	Level   uint64
	Items   []Item
	Friends []uint64
}
type Item struct {
	Id   uint64
	Type string
	Data string
}

func NewUser() *User {
	it := make([]Item, 20)
	for i, _ := range it {
		it[i].Id = 1000 + uint64(i)
		it[i].Type = "sometype"
		it[i].Data = "some data blah blah blah"
	}
	friends := make([]uint64, 50)
	for i, _ := range friends {
		friends[i] = uint64(i) + 10000000
	}
	user := &User{
		Id:      1292983,
		Name:    "Vinay",
		Xp:      100,
		Level:   200,
		Items:   it,
		Friends: friends,
	}
	return user
}

var obj interface{}
var key string

func responseHandler(w http.ResponseWriter, r *http.Request) {
	client := pool.Get()
    defer pool.Release(client)
	load(client, key, w)
	//log.Print(obj)
	compute()
	//obj = NewUser()
	//save(client, key, zr, w)
	fmt.Fprintf(w, "OK! %s", key)
}

func main() {
	runtime.GOMAXPROCS(16)
	key = "user_data_2"
	spec := redis.DefaultSpec().Db(0).Host("10.174.178.235")
	primeClient, err := redis.NewAsynchClientWithSpec(spec)
	if err != nil {
		panic(err)
	}
	primeKey(key, primeClient)
	log.Println("Key primed and ready");
	defer primeClient.Quit()
	pool = NewClientPool(100)
	handler := func(w http.ResponseWriter, r *http.Request) {
		responseHandler(w, r)
	}
	http.HandleFunc("/", handler)
	http.ListenAndServe(":80", nil)
}


func NewRedisAsyncClient() redis.AsyncClient {
	spec := redis.DefaultSpec().Db(0).Host("10.174.178.235")
	client, err := redis.NewAsynchClientWithSpec(spec)
	if err != nil {
		panic(err)
	}
	return client
}

func primeKey(key string, r redis.AsyncClient){
		path := "document.json";
	    file, err := os.Open(path);
	    if err != nil {
	    	panic(err)
	    }
        reader := bufio.NewReader(file)
		document, _ := ioutil.ReadAll(reader)
		var b bytes.Buffer
		z := zlib.NewWriter(&b)
		z.Write(document)
		z.Close();
		f, rerr := r.Set(key, b.Bytes())
		if rerr != nil {
			panic(rerr)
		}
		_, rerr, timeout := f.TryGet(50000000000)
		if rerr != nil {
			panic(rerr)
		}
		if timeout {
			savetimeout++
			log.Println("load timeout! count: ", savetimeout)
		}
}