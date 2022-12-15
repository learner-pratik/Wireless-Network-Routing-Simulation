rm inputs/* outputs/* data/* &
java Controller 100 &
java Node 0 100 2 "message from 0 to 2" &
java Node 1 100 -1 &
java Node 2 100 -1 &
java Node 3 100 -1