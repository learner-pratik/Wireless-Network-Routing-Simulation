rm inputs/* outputs/* data/* &
java Controller 100 &
java Node 0 100 3 "message from 0 to 3" &
java Node 1 100 -1 &
java Node 2 100 4 "message from 2 to 4" &
java Node 3 100 -1 &
java Node 4 100 -1