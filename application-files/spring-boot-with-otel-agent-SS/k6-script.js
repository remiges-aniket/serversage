import http from 'k6/http';
import { sleep } from 'k6';

export default function () {
  // var server_list = ["localhost:8000", "localhost:8081", "localhost:3001"]
  var server_list = ["localhost:8081"]
  var endpoint_list = ["/init", "/order","/order/{id}","order_list","/total_order","/rejected_order","/cpu_task", "/random_sleep", "/random_status", "/chain", "/error_test"]
  server_list.forEach(function(server) {
    endpoint_list.forEach(function(endpoint) {
      http.get("http://" + server + endpoint);
      if (endpoint == "/order" ) {
        http.put("http://" + server + endpoint);
        if (getRandom(1, 20) < 10 ){
          http.put("http://" + server + endpoint);
          http.del("http://" + server + endpoint);
        }
        http.post("http://" + server + endpoint);
      }
    });
  });
  sleep(0.5);
}

function getRandom(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}