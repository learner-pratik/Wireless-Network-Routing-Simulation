rm inputs/* outputs/* logs/* data/* &
java Controller 100 &
java Node 0 100 3 message_from_0_to_3 &
java Node 1 100 -1 &
java Node 2 100 -1 &
java Node 3 100 -1 &
java Node 4 100 -1