package fdi.test.lucene;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

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
	private LinkedList<String> StopApliqued;
	
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
		
		HashMap<Long, String> Grammar2Nombre=new HashMap<Long, String>();
		
		Stack<CompleteElementType> Pendientes=new Stack<CompleteElementType>();
		
		for (CompleteGrammar gramm : object.getMetamodelGrammar()) {
			Pendientes.addAll(gramm.getSons());

			String Bane = gramm.getNombre().toLowerCase();
			Bane=CleanRaro(Bane);
			Grammar2Nombre.put(gramm.getClavilenoid(), Bane);
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
			
			String Bane = Act.getName().toLowerCase();
			Bane=CleanRaro(Bane);
			
			ClaseID2Nombre.put(ID, Bane);
			
			
			if (Act.getCollectionFather()!=null)
				ElemtId2Grammar.put(ID, Act.getCollectionFather().getClavilenoid());
			
			Pendientes.addAll(Act.getSons());
		}
		
		
		if (debug)
		{
			System.out.println("//GRAMS");
			for (Entry<Long, String> completeGramPair : Grammar2Nombre.entrySet()) {
				System.out.println(completeGramPair.getKey()+": "+completeGramPair.getValue());
			}
			
			System.out.println("//TYPES");
			for (Entry<Long, String> completeElementTypePair : ClaseID2Nombre.entrySet()) {
				System.out.println(completeElementTypePair.getKey()+": "+completeElementTypePair.getValue());
			}
			
			System.out.println("//DOCS");
			for (CompleteDocuments document : object.getEstructuras()) {
				System.out.println("ID: "+document.getClavilenoid());
				System.out.println("-->"+document.getDescriptionText().toLowerCase());
				for (CompleteElement element : document.getDescription()) {
					System.out.print("-->..>"+element.getHastype().getName().toLowerCase());

					 String Value=null;
					 
					 if (element instanceof CompleteTextElement)
						 Value=((CompleteTextElement) element).getValue();
					 
					 if (element instanceof CompleteResourceElementURL)
						 Value=((CompleteResourceElementURL) element).getValue();
					 
					 if (element instanceof CompleteResourceElementFile)
						 Value=((CompleteResourceElementFile) element).getValue().getPath();
					
					 if (Value!=null)
						 System.out.println(": "+Value.toLowerCase());
				}
			}
		}
		
		
		 ClasePrincipal C = new ClasePrincipal();
		C.process(object.getEstructuras(),ElemtId2ClaseID,ElemtId2Grammar,ClaseID2Nombre,Grammar2Nombre);
		
		boolean salida=false;
		String texto;
		
		Scanner sc = new Scanner(System.in);
		System.out.println("introduce la querry");
		while(!salida)
		{
			
			
			texto= sc.next().toLowerCase();
			
			if (texto.equals("exit()"))
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

	private static String CleanRaro(String bane) {
		String Nuevo = new String(bane);
		
		Nuevo=Nuevo.replace('á', 'a');
		Nuevo=Nuevo.replace('à', 'a');
		Nuevo=Nuevo.replace('é', 'e');
		Nuevo=Nuevo.replace('è', 'e');
		Nuevo=Nuevo.replace('í', 'i');
		Nuevo=Nuevo.replace('ì', 'i');
		Nuevo=Nuevo.replace('ó', 'o');
		Nuevo=Nuevo.replace('ò', 'o');
		Nuevo=Nuevo.replace('ú', 'u');
		Nuevo=Nuevo.replace('ù', 'u');
		
		return Nuevo;
	}

	private void process(List<CompleteDocuments> estructuras, HashMap<Long, Long> elemtId2ClaseID,
			HashMap<Long, Long> elemtId2Grammar, HashMap<Long, String> claseID2Nombre, HashMap<Long, String> grammar2Nombre) {
		
		StandardAnalyzer analyzer = new StandardAnalyzer();
		
		try {
		 Directory index = 
		         FSDirectory.open(Paths.get("/tmp/luceneExample"));

		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(OpenMode.CREATE);
		
			IndexWriter w = new IndexWriter(index, config);
			
			try {
				
				for (CompleteDocuments completeDocument : estructuras) {
					addDoc(w,completeDocument,elemtId2ClaseID,elemtId2Grammar,claseID2Nombre,grammar2Nombre);
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
			HashMap<Long, Long> elemtId2Grammar, HashMap<Long, String> claseID2Nombre, HashMap<Long, String> grammar2Nombre) throws IOException {
		
		 Document doc = new Document();
		 
		 String Descripcion= completeDocument.getDescriptionText().toLowerCase();
		 
		 List<Character> DescList=new LinkedList<Character>();
		
		 for (int i = 0; i < Descripcion.length(); i++) 
				if ((Descripcion.charAt(i)>='a'&&Descripcion.charAt(i)<='z')||(Descripcion.charAt(i)>='0'&&Descripcion.charAt(i)<='9')||Descripcion.charAt(i)==' ')
					DescList.add(Descripcion.charAt(i));
		 
		 
		 StringBuffer SBA=new StringBuffer();
		 
		 for (Character chr : DescList) 
			SBA.append(chr);
		
		 String[] ListaTA = SBA.toString().split(" ");
		 
		 List<String> listaT=new ArrayList<String>();
		 
		 for (String string : ListaTA)
			listaT.add(string);
		
		 listaT.removeAll(StopApliqued);
		 
		 StringBuffer SB=new StringBuffer();
		 for (String string : listaT) {
			SB.append(string);
			SB.append(" ");
		}
		 
		Descripcion=SB.toString().trim(); 
		 
		 
		 
		 doc.add(new TextField("ALL", Descripcion , Field.Store.YES));
		 
		 HashSet<Long> Gramatica=new HashSet<Long>();
		 
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
				 Value=Value.toLowerCase();
				
				 
				 
				 List<Character> DescListVal=new LinkedList<Character>();
					
				 for (int i = 0; i < Value.length(); i++) 
						if ((Value.charAt(i)>='a'&&Value.charAt(i)<='z')||(Value.charAt(i)>='0'&&Value.charAt(i)<='9')||Value.charAt(i)==' ')
							DescListVal.add(Value.charAt(i));				 

				 StringBuffer SBAV=new StringBuffer();
				 
				 for (Character chr : DescListVal) 
					SBAV.append(chr);
				
				 String[] ListaTAV = SBAV.toString().split(" ");
				 
				 List<String> listaTV=new ArrayList<String>();
				 
				 for (String string : ListaTAV)
					listaTV.add(string);
				
				 listaTV.removeAll(StopApliqued);
				 
				 StringBuffer SBV=new StringBuffer();
				 for (String string : listaTV) {
					SBV.append(string);
					SBV.append(" ");
				}
				 
				Value=SBV.toString().trim(); 
				 
				 Long IDclass=elemtId2ClaseID.get(IDL.getClavilenoid());
				 
				 if (IDclass!=null)
					 doc.add(new TextField(IDclass.toString(), Value, Field.Store.YES));
				 
				 String IDclassName=claseID2Nombre.get(IDL.getClavilenoid());
				 
				 if (IDclassName!=null)
					 doc.add(new TextField(IDclassName.toLowerCase(), Value, Field.Store.YES));
				 
				 
				 Long LongGram = elemtId2Grammar.get(IDL.getClavilenoid());
				 
				 if (LongGram!=null)
				 	Gramatica.add(LongGram);
			 }
			 
			 
			
			 
			}
		 
		 
		 for (Long long1 : Gramatica) {
			 String name=grammar2Nombre.get(long1);
			 if (name!=null&&!name.isEmpty())
				 doc.add(new TextField("0", name, Field.Store.YES));
		}

		  w.addDocument(doc);
		
		
		
	}

	private QueryParser queryParser;
	private IndexSearcher indexSearcher;
	private Query query;
	
	public ClasePrincipal() {
		 String resourceName = "stopwords-all.json";
	        Reader is;
	        StopApliqued = new LinkedList<String>();
			try {
				is = new FileReader(resourceName);
				 JSONTokener tokener = new JSONTokener(is);
			     JSONObject object = new JSONObject(tokener);
			     JSONArray listaIngles = object.getJSONArray("es");
			     for (int i = 0; i < listaIngles.length(); i++)
			    	 StopApliqued.add(listaIngles.getString(i));
				
			    
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
	       
//			for (String string : StopApliqued) 
//				System.out.println(string);
			
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
