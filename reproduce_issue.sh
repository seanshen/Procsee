#!/bin/bash

echo "Request 1: Set testAttr = 100"
curl -v -c cookies.txt -X POST -H "Content-Type: application/json" -d '{"testAttr": 100}' http://localhost:8080/api/session
echo -e "\n"

echo "Request 2: Get testAttr"
curl -v -b cookies.txt http://localhost:8080/api/session
echo -e "\n"

echo "Request 3: Set testAttr = 200"
curl -v -b cookies.txt -X POST -H "Content-Type: application/json" -d '{"testAttr": 200}' http://localhost:8080/api/session
echo -e "\n"

echo "Request 4: Get testAttr"
curl -v -b cookies.txt http://localhost:8080/api/session
echo -e "\n"
