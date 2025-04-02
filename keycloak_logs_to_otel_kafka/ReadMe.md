 
Go to below URL to check the keycloack metrics: (same will reflect in prometheus-> explore)

    curl http://localhost:8080/metrics


refer: https://github.com/kokuwaio/keycloak-event-metrics

Need output in above curl command as below:

    keycloak_event_user_total{client="test",realm="9039a0b5-e8c9-437a-a02e-9d91b04548a4",type="LOGIN",error="",} 2.0
    keycloak_event_user_total{client="test",realm="1fdb3465-1675-49e8-88ad-292e2f42ee72",type="LOGIN",error="",} 1.0
    keycloak_event_user_total{client="test",realm="1fdb3465-1675-49e8-88ad-292e2f42ee72",type="LOGIN_ERROR",error="invalid_user_credentials",} 1.0
    keycloak_event_user_total{client="UNKNOWN",realm="1fdb3465-1675-49e8-88ad-292e2f42ee72",type="LOGIN_ERROR",error="invalid_user_credentials",} 1.0