#!/bin/bash

#Compilation line: javac -cp .:cplex/cplex.jar *.java
#Execute line: java -cp .:cplex/x86-64_linux Main <FileName> <K> <Epsilon>

export LD_LIBRARY_PATH=cplex/x86-64_linux
eps=0
for k in {20,30}
# ,10,20,25,30,40,50}
do  
for e in {5,10}
# ,1,2,5}
do
for f in input/*.txt 
do 
	eps="$(echo "($k/100)*$e" | bc -l)"
	echo "Processing $f file..."
	java -cp .:cplex/cplex.jar Main "$f" "$k"% "$eps"%
done
done
done

