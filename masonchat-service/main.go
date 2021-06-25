package main

import (
	"flag"
	"github.com/gorilla/mux"
	"log"
	"net/http"
	"time"
)

var addr = flag.String("addr", ":8080", "http service address")

func serveHome(w http.ResponseWriter, r *http.Request) {
	log.Println(r.URL)
	http.ServeFile(w, r, "chat.html")
}

func main() {
	flag.Parse()

	hub := newHub()
	go hub.start()

	r := mux.NewRouter()
	r.HandleFunc("/", serveHome).Methods("GET")
	r.HandleFunc("/chat/{nick}", hub.addClient).Methods("GET")

	srv := &http.Server{
		Handler:      r,
		Addr:         *addr,
		// Good practice: enforce timeouts for servers you create!
		WriteTimeout: 15 * time.Second,
		ReadTimeout:  15 * time.Second,
	}

	log.Fatal(srv.ListenAndServe())
}