
### Assignment-2-Distributed-Systems-a1911465
# Building an aggregation server with consistency management and a RESTful API.
## Overview

This project implements a distributed weather data system consisting of three components: the **Aggregation Server**, the **Content Server**, and the **GET Client**. Each component uses a **Lamport clock** to maintain synchronization and consistency across the system.

### Components

1. **Aggregation Server**
    - Responsible for handling incoming connections from content servers and processing weather data.
    - Maintains a persistent storage of weather information.
    - Automatically cleans up stale data after 30 seconds of inactivity.

2. **Content Server**
    - Reads weather data from a specified file, converts it into JSON format, and sends it to the Aggregation Server.

3. **GET Client**
    - Sends GET requests to the Aggregation Server to retrieve weather data and display it in a readable format.

## Assignment Expectations

- **Lamport Clock Implementation**: Each component uses a Lamport clock to ensure that operations are executed in a consistent manner across the distributed system.

- **HTTP Request Handling**: The Aggregation Server handles GET and PUT requests appropriately, returning the correct HTTP status codes:
    - `201 Created` for the first successful upload of data.
    - `200 OK` for subsequent updates.
    - `400 Bad Request` for unsupported request methods.
    - `204 No Content` for requests that yield no data.
    - `500 Internal Server Error` for invalid JSON data.

- **File Handling**: Weather data is stored in a JSON format in a persistent file, which survives server crashes. The server can recover its state upon restarting.

## Usage

### Running the Aggregation Server

NOTE: Please note the filepath (Kindly update file path as per the project destination on your system )

To start the Aggregation Server, navigate to the project directory in your terminal and run the following command:


```bash
java AggregationServer 4567
```

After executing this command, you should see output indicating that the server has started, similar to:

```
Aggregation Server started on port 4567
```

The server will continuously run, listening for incoming connections from the Content Server and GET Client. You will see logs in green color indicating connections received and any relevant status updates.

### Running the Content Server

To start the Content Server, use the following command:

```bash
java ContentServer <server-url:port> <file-path>
```

For example:

```bash
java ContentServer localhost:4567 /path/to/weather_data.txt
```

### Running the GET Client

To run the GET Client, execute:

```bash
java GETClient <server-url:port> [stationId]
```

For example:

```bash
java GETClient localhost:4567 TestStation
```


### Clean Up

Make sure to delete any temporary data files generated during testing, especially in the `data/` directory. This can be done manually or through a cleanup script.

## Lamport Clock Implementation

The Lamport clock is implemented in all components of the system. It is used to ensure that events in the distributed system are timestamped in a manner that respects the causality of events. Each time an event occurs (such as sending or receiving a request), the Lamport clock is incremented, ensuring a consistent logical ordering of events across the system.

### Example of Bash Output

When running the Aggregation Server, the Bash terminal will display output similar to the following, indicating the status of the server:

```bash
java AggregationServer 4567
Aggregation Server started on port 4567
Received connection from client: /192.168.1.2:54321
```
### Example of content server output
```bash
Sending JSON Data: {"id":"IDS60901","name":"Adelaide (West Terrace / ngayirdapira)","state":"SA","time_zone":"CST","lat":-34.9,"lon":138.6,"local_date_time":"15/04:00pm","local_date_time_full":"20230715160000","air_temp":13.3,"apparent_t":9.5,"cloud":"Partly cloudy","dewpt":5.7,"press":1023.9,"rel_hum":60,"wind_dir":"S","wind_spd_kmh":15,"wind_spd_kt":8}
Server Response: HTTP/1.1 200 OK

Process finished with exit code 0
```
### Example of GETClient output
```bash
Request sent:
GET /weather HTTP/1.1
Host: localhost:4567

Full Server Response: Weather{id:'IDS60901', name:'Adelaide (West Terrace / ngayirdapira)', state:'SA', time_zone:'CST', lat:-34.9, lon:138.6, local_date_time:'15/04:00pm', local_date_time_full:'20230715160000', air_temp:13.3, apparent_t:9.5, cloud:'Partly cloudy', dewpt:5.7, press:1023.9, rel_hum:60.0, wind_dir:'S', wind_spd_kmh:15.0, wind_spd_kt:8.0}
Valid JSON Response: {id:'IDS60901', name:'Adelaide (West Terrace / ngayirdapira)', state:'SA', time_zone:'CST', lat:-34.9, lon:138.6, local_date_time:'15/04:00pm', local_date_time_full:'20230715160000', air_temp:13.3, apparent_t:9.5, cloud:'Partly cloudy', dewpt:5.7, press:1023.9, rel_hum:60.0, wind_dir:'S', wind_spd_kmh:15.0, wind_spd_kt:8.0}
Weather Data:
id: 'IDS60901'
name: 'Adelaide (West Terrace / ngayirdapira)'
state: 'SA'
time_zone: 'CST'
lat: -34.9
lon: 138.6
local_date_time: '15/04:00pm'
local_date_time_full: '20230715160000'
air_temp: 13.3
apparent_t: 9.5
cloud: 'Partly cloudy'
dewpt: 5.7
press: 1023.9
rel_hum: 60.0
wind_dir: 'S'
wind_spd_kmh: 15.0
wind_spd_kt: 8.0

Process finished with exit code 0
```

These logs help to monitor the activity and connections handled by the server in real time.


## Conclusion

This project meets the requirements outlined in the assignment by implementing a functional distributed weather data system with proper synchronization, file handling, and error management using Lamport clocks.

