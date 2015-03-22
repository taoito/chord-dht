rm -fr bin/*

javac -cp bin -d bin src/*.java


#java -Djava.security.policy=src/java/policyfile.txt PowerServiceServer

#TO start the client
#  java -cp bin/ -Djava.security.policy=src/java/policyfile.txt ClientChord localhost 1900


#To start the SuperNode Server
# java -cp bin/ -Djava.security.policy=src/policyfile.txt SuperNode


# All the code was tested at host name: kh2170-09.cselabs.umn.edu 

#Syntax to start the NodeDHT:
#NodeDHT LocalPortNumber SuperNode-HostName  
# - LocalPortNumber is the number that will receive connections in the Node
# - SuperNode-Hostname is the host name of the SuperNode 

#NodeDHT 1
# java -cp bin/ -Djava.security.policy=src/policyfile.txt NodeDHT 55511 localhost

#NodeDHT 2
# java -cp bin/ -Djava.security.policy=src/policyfile.txt NodeDHT 55512 localhost

#NodeDHT 3
# java -cp bin/ -Djava.security.policy=src/policyfile.txt NodeDHT 55513 localhost

#NodeDHT 4
# java -cp bin/ -Djava.security.policy=src/policyfile.txt NodeDHT 55514 localhost

#NodeDHT 5
# java -cp bin/ -Djava.security.policy=src/policyfile NodeDHT 55515 128.101.39.165
