package fdi.test.lucene;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.document.Field;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;

public class ClasePrincipal {

	
	private static final String DEFAULT_SEARCH_FIELD = "title";
	private static final int MAX_SEARCH = 1000;

	public static void main(String[] args) {
		 ClasePrincipal C = new ClasePrincipal();
		C.process();
		try {
			TopDocs result = C.search("lucene");
			for (ScoreDoc string : result.scoreDocs) {
				System.out.println(C.getDocument(string));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
	}

	private QueryParser queryParser;
	private IndexSearcher indexSearcher;
	private Query query;
	
	public ClasePrincipal() {
		// TODO Auto-generated constructor stub
	}

	private void process() {
		StandardAnalyzer analyzer = new StandardAnalyzer();
		
		try {
		 Directory index = 
		         FSDirectory.open(Paths.get("/tmp/luceneExample"));

		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		
			IndexWriter w = new IndexWriter(index, config);
			
			try {
				addDoc(w, "Lucene in Action", "193398817");
				addDoc(w, "Lucene for Dummies", "55320055Z");
				addDoc(w, "Managing Gigabytes", "55063554A");
				addDoc(w, "The Art of Computer Science", "9900333X");
			} catch (Exception e2) {
				e2.printStackTrace();
			}
			
			w.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			 Directory indexDirectory = 
			         FSDirectory.open(Paths.get("/tmp/luceneExample"));
			      IndexReader reader = DirectoryReader.open(indexDirectory);
			      indexSearcher = new IndexSearcher(reader);
			      queryParser = new QueryParser(DEFAULT_SEARCH_FIELD,
			         new StandardAnalyzer());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		
	}
	
	
	
	public TopDocs search( String searchQuery) 
		      throws IOException, ParseException {
		      query = queryParser.parse(searchQuery);
		      return indexSearcher.search(query, MAX_SEARCH);
		   }
		   public Document getDocument(ScoreDoc scoreDoc) 
		      throws CorruptIndexException, IOException {
		      return indexSearcher.doc(scoreDoc.doc);	
		   }
	
	
	private static void addDoc(IndexWriter w, String title, String isbn) throws IOException {
		  Document doc = new Document();
		  doc.add(new TextField("title", title, Field.Store.YES));
		  doc.add(new StringField("isbn", isbn, Field.Store.YES));
		  w.addDocument(doc);
		}
}
