# **Project Title**
**Positional Index**

## Objective
- **Part_1:** Build positional index from the attached dataset and display each term as:  
< term &nbsp; doc1: p1, p2, ...; doc2: p1, p2, ...; etc. >
- **Part_2:** Use the MapReduce job output file from *Part_1*
    - Compute term-frequency for each term.
    - Compute IDF for each term.
    - Compute TF.IDF matrix for each term.
    - Allow the users to enter phrase queries on the positional index, then compute the similarity between the query and the matched documents. And then, rank the documents based on their similarity scores, and return the relevant documents for the query. The phrase query can include boolean operators.
