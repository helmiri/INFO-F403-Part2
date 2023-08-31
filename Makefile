all:
	jflex src/LexicalAnalyzer.flex -d src/parser/
	javac -d bin -cp src/ src/Main.java
	jar cfe dist/part2.jar Main -C bin .

testing:
	java -jar dist/part2.jar -wt tree.tex test/Factorial.fs
	pdflatex tree.tex > /dev/null
