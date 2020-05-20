package fdi.test.lucene;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Stack;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

import fdi.ucm.server.modelComplete.collection.CompleteCollection;
import fdi.ucm.server.modelComplete.collection.document.CompleteDocuments;
import fdi.ucm.server.modelComplete.collection.document.CompleteElement;
import fdi.ucm.server.modelComplete.collection.document.CompleteResourceElementFile;
import fdi.ucm.server.modelComplete.collection.document.CompleteResourceElementURL;
import fdi.ucm.server.modelComplete.collection.document.CompleteTextElement;
import fdi.ucm.server.modelComplete.collection.grammar.CompleteElementType;
import fdi.ucm.server.modelComplete.collection.grammar.CompleteGrammar;

import org.apache.lucene.document.Field;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;

public class ClasePrincipal {

	
	private static final String DEFAULT_SEARCH_FIELD = "title";
	private static final int MAX_SEARCH = 1000;

	public static void main(String[] args) {
		
		if (args.length<1)
			{
			System.err.println("Es necesario introducir algun archivo clavy");
			System.exit(-1);
			}
		
		CompleteCollection object=null;
		
		try {
			 File file = new File(args[0]);
			 FileInputStream fis = new FileInputStream(file);
			 ObjectInputStream ois = new ObjectInputStream(fis);
			 object = (CompleteCollection) ois.readObject();
			 ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		boolean debug = false;
		
		if (args.length>1)
			try {
				debug=Boolean.parseBoolean(args[1]);
			} catch (Exception e) {
				
			}
		
		
		HashMap<Long, Long> ElemtId2ClaseID=new HashMap<Long, Long>();
		
		HashMap<Long, Long> ElemtId2Grammar=new HashMap<Long, Long>();
		
		HashMap<Long, String> ClaseID2Nombre=new HashMap<Long, String>();
		
		
		Stack<CompleteElementType> Pendientes=new Stack<CompleteElementType>();
		
		for (CompleteGrammar gramm : object.getMetamodelGrammar()) {
			Pendientes.addAll(gramm.getSons());
		}
		
		
		while(!Pendientes.isEmpty())
		{
			CompleteElementType Act = Pendientes.pop();
			
			Long IDClass=0l;
			Long ID=0l;
			
			if (Act.getClassOfIterator()==null)
				IDClass=Act.getClavilenoid();
			else
				IDClass=Act.getClassOfIterator().getClavilenoid();
			
			ID=Act.getClavilenoid();
			
			ElemtId2ClaseID.put(ID, IDClass);
			
			ClaseID2Nombre.put(ID, Act.getName());
			
			if (Act.getCollectionFather()!=null)
				ElemtId2Grammar.put(ID, Act.getCollectionFather().getClavilenoid());
			
			Pendientes.addAll(Act.getSons());
		}
		
		
		if (debug)
		{
			System.out.println("//TYPES");
			for (Entry<Long, String> completeElementTypePair : ClaseID2Nombre.entrySet()) {
				System.out.println(completeElementTypePair.getKey()+": "+completeElementTypePair.getValue());
			}
			
			System.out.println("//DOCS");
			for (CompleteDocuments document : object.getEstructuras()) {
				System.out.println("ID: "+document.getClavilenoid());
			}
		}
		
		
		 ClasePrincipal C = new ClasePrincipal();
		C.process(object.getEstructuras(),ElemtId2ClaseID,ElemtId2Grammar,ClaseID2Nombre);
		
		boolean salida=false;
		String texto;
		
		Scanner sc = new Scanner(System.in);
		System.out.println("introduce la querry");
		while(!salida)
		{
			
			
			texto= sc.next();
			
			if (texto.toLowerCase().equals("exit()"))
			{
				salida=true;
				break;
			}
			
			try {
				TopDocs result = C.search(texto);
				System.out.println("Found: "+result.totalHits);
				for (ScoreDoc string : result.scoreDocs) {
					System.out.println(C.getDocument(string));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
		
		sc.close();
		
		
	}

	private void process(List<CompleteDocuments> estructuras, HashMap<Long, Long> elemtId2ClaseID,
			HashMap<Long, Long> elemtId2Grammar, HashMap<Long, String> claseID2Nombre) {
		
		StandardAnalyzer analyzer = new StandardAnalyzer();
		
		try {
		 Directory index = 
		         FSDirectory.open(Paths.get("/tmp/luceneExample"));

		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(OpenMode.CREATE);
		
			IndexWriter w = new IndexWriter(index, config);
			
			try {
				
				for (CompleteDocuments completeDocument : estructuras) {
					addDoc(w,completeDocument,elemtId2ClaseID,elemtId2Grammar,claseID2Nombre);
				}
	
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
			      queryParser = new QueryParser("ALL",
			         new StandardAnalyzer());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void addDoc(IndexWriter w, CompleteDocuments completeDocument, HashMap<Long, Long> elemtId2ClaseID,
			HashMap<Long, Long> elemtId2Grammar, HashMap<Long, String> claseID2Nombre) throws IOException {
		
		 Document doc = new Document();
		 
		 doc.add(new TextField("ALL", completeDocument.getDescriptionText(), Field.Store.YES));
		 
		 for (CompleteElement elem : completeDocument.getDescription()) {
			
			 CompleteElementType IDL = elem.getHastype();
			 
			 String Value=null;
			 
			 if (elem instanceof CompleteTextElement)
				 Value=((CompleteTextElement) elem).getValue();
			 
			 if (elem instanceof CompleteResourceElementURL)
				 Value=((CompleteResourceElementURL) elem).getValue();
			 
			 if (elem instanceof CompleteResourceElementFile)
				 Value=((CompleteResourceElementFile) elem).getValue().getPath();
			 
			 if (IDL!=null&&Value!=null&&!Value.isEmpty())
			 {
				 Long IDclass=elemtId2ClaseID.get(IDL.getClavilenoid());
				 
				 if (IDclass!=null)
					 doc.add(new TextField(IDclass.toString(), Value, Field.Store.YES));
				 
				 String IDclassName=claseID2Nombre.get(IDL.getClavilenoid());
				 
				 if (IDclassName!=null)
					 doc.add(new TextField(IDclassName, Value, Field.Store.YES));
			 }
			 
			}

		  w.addDocument(doc);
		
		
		
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
		config.setOpenMode(OpenMode.CREATE);
		
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
