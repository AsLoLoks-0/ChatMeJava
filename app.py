import eventlet
import eventlet.wsgi
from flask import Flask, render_template
import socketio
import json


instrument = False

sio = socketio.Server(
    async_mode='eventlet',
    cors_allowed_origins=None if not instrument else [
        'http://localhost:8080',
        'https://admin.socket.io',  # edit the allowed origins if necessary
    ])


app = Flask(__name__)
app.wsgi_app = socketio.WSGIApp(sio, app.wsgi_app)
app.config['SECRET_KEY'] = 'secret!'
thread = None

def background_thread():
    """Example of how to send server generated events to clients."""
    count = 0
    while True:
        sio.sleep(10)
        count += 1
        sio.emit('my_response', {'data': 'Server generated event'})

# @sio.event
# def android(sid, message):
#     print("tactical banana " + message + " from android")
#     print(message["name"])
#     sio.emit('my_response', {'data': 'it is working'}, room=sid)

# @sio.event
# def bet(sid, message):
#     print("bet " + message)

# @sio.event
# def call(sid, message):
#     print("call " + message)

# @sio.event
# def check(sid, message):
#     print("check " + message)

# @sio.event
# def fold(sid, message):
#     print("fold " + message)

# @sio.event
# def join_room(sid, message):
#     print("join room " + message)

@sio.event
def client_to_server_message(sid, message):
    sio.emit('server_respond', json.dumps(message), room=message['room'])

@sio.event
def exit_room(sid, message):
    sio.leave_room(sid, message)
    print(sid + " exited room " + message)

@sio.event
def join_room(sid, message):
    sio.enter_room(sid, message)
    print(sid + " joined room " + message)




@sio.event
def disconnect_request(sid):
    sio.disconnect(sid)

@sio.event
def connect(sid, environ):
    sio.emit('my_response', {'data': 'Connected', 'count': 0}, room=sid)

@sio.event
def disconnect(sid):
    print('Client disconnected')


if __name__ == '__main__':
        eventlet.wsgi.server(eventlet.listen(('', 8080)), app)

