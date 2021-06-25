package main

import (
	"github.com/gorilla/mux"
	"github.com/gorilla/websocket"
	"log"
	"net/http"
)

// Hub maintains the set of active clients and broadcasts messages to the
// clients.
type Hub struct {
	// Registered clients.
	clients map[*Client]bool

	// Inbound messages from the clients.
	broadcast chan []byte

	// Register requests from the clients.
	register chan *Client

	// Unregister requests from clients.
	unregister chan *Client

	upgrader websocket.Upgrader

}

func newHub() *Hub {
	return &Hub{
		broadcast:  make(chan []byte),
		register:   make(chan *Client),
		unregister: make(chan *Client),
		clients:    make(map[*Client]bool),
		upgrader: websocket.Upgrader{
			ReadBufferSize:    1024,
			WriteBufferSize:   1024,
			EnableCompression: true,
		},
	}
}

func (h *Hub) start() {
	for {
		select {
		case client := <-h.register:
			h.clients[client] = true
		case client := <-h.unregister:
			if _, ok := h.clients[client]; ok {
				delete(h.clients, client)
				close(client.send)
			}
		case message := <-h.broadcast:
			for client := range h.clients {
				select {
				case client.send <- message:
				default:
					close(client.send)
					delete(h.clients, client)
				}
			}
		}
	}
}

// serveWs handles websocket requests from the peer.
func (h *Hub) addClient(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	nick, found := vars["nick"]
	if (!found) {
		http.Error(w, "nick is required", http.StatusInternalServerError)
		log.Println("No nick specifified")
		return
	}

	conn, err := h.upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println(err)
		return
	}

	client := &Client{hub: h, conn: conn, nick: nick, send: make(chan []byte, 256)}
	client.hub.register <- client

	client.send <- []byte("Server:Welcome to MasonChat!")

	client.start()

	log.Printf("Client connected: %s", client.nick)
}