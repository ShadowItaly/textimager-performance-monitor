package org.hucompute.textimager.performance_monitor;

import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import org.hucompute.textimager.uima.biofid.flair.BiofidFlair;
import org.dkpro.core.languagetool.LanguageToolSegmenter;
import org.hucompute.textimager.uima.steps.StepsParser;
import java.time.LocalTime; // import the LocalTime class

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
import java.time.LocalDate; // import the LocalDate class

import org.json.JSONArray;
import org.json.JSONObject;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import java.lang.Runtime;
import java.lang.Process;
import java.io.*;
import java.time.Instant;


class TestResult {
  public long _executionTime;
  public String _document_text;
  public long  _numSentences;
  public int _batch_size;
  public String _document_language;
  public boolean _gpu;

  public TestResult(long time, String document_text, int batch_size, String document_language, boolean gpu) {
    _executionTime = time;
    _document_text = document_text;
    _numSentences = document_text.split("\n").length;
    _batch_size = batch_size;
    _document_language = document_language;
    _gpu = gpu;
  }

  JSONObject toJson() {
    JSONObject obj = new JSONObject();
    obj.put("runtime",_executionTime);
    obj.put("document_text",_document_text);
    obj.put("lines",_numSentences);
    obj.put("batch_size",_batch_size);
    obj.put("language",_document_language);
    obj.put("gpu",_gpu);
    return obj;
  }
}

/**
 * Hello world!
 *
 */
public class App 
{
  private Vector<TestResult> _results;
  private static int NUM_ITERATIONS_TOTAL = 1;
  private static int NUM_ITERATIONS_WARMUP = 0;

    public App() {
      _results = new Vector<TestResult>();
    }

    public void annotate_sentences(JCas jc, String document_text) {
        String simple = "[.?!]";
        String[] splitString = (document_text.split(simple));
        int start = 0;
        for(String x : splitString) {
          Sentence sentence1 = new Sentence(jc, start, start+x.length());
          start+=x.length()+1;
          sentence1.addToIndexes();
        }
    }

    public void run_test(String document_text, int batch_size, String document_language, boolean gpu) throws UIMAException, IOException {
        JCas jCas = JCasFactory.createText(document_text,
                document_language);

        AnalysisEngineDescription segmenter = createEngineDescription(LanguageToolSegmenter.class);

     
        //AnalysisEngineDescription stepsParser = createEngineDescription(StepsParser.class,
        //        StepsParser.PARAM_REST_ENDPOINT, "http://localhost:8000"
        //);
            
        AnalysisEngineDescription biodidFlairTagger = createEngineDescription(BiofidFlair.class,
          BiofidFlair.PARAM_REST_ENDPOINT, "http://localhost:5567;http://localhost:5568;http://localhost:5569"
        );

        BiofidFlair.set_batch_size(jCas,batch_size);
        annotate_sentences(jCas,document_text);
        System.out.println(document_text);
        long startTime = 0;
        for(int i = 0; i < NUM_ITERATIONS_TOTAL; i++) {
          if(i==NUM_ITERATIONS_WARMUP) {
            startTime = System.nanoTime();
          }
          SimplePipeline.runPipeline(jCas, segmenter, biodidFlairTagger);
          jCas.reset();
          jCas.setDocumentText(document_text);
          jCas.setDocumentLanguage(document_language);
          BiofidFlair.set_batch_size(jCas,batch_size);
          annotate_sentences(jCas,document_text);
        }
        long total = (System.nanoTime()-startTime)/(NUM_ITERATIONS_TOTAL-NUM_ITERATIONS_WARMUP);
        _results.add(new TestResult(total,document_text,batch_size,document_language, gpu));
    }
    
    public void write_results(boolean gpu) throws IOException {
      JSONObject outer = new JSONObject();
      JSONArray arr = new JSONArray();
      for(int i = 0; i < _results.size(); i++) {
        arr.put(_results.get(i).toJson());
      }
      File writer = new File("output.json");
      FileWriter wr = new FileWriter(writer);
      outer.put("data",arr);
      outer.put("iterations_total",App.NUM_ITERATIONS_TOTAL);
      outer.put("iterations_warmup",App.NUM_ITERATIONS_WARMUP);

      SystemInfo systemInfo = new SystemInfo();
      HardwareAbstractionLayer hardware = systemInfo.getHardware();
      CentralProcessor processor = hardware.getProcessor();
      
      CentralProcessor.ProcessorIdentifier processorIdentifier = processor.getProcessorIdentifier();

      outer.put("processor",processor.toString());
      outer.put("date",Instant.now().toString());

      
      outer.put("cpu_name", processorIdentifier.getName());
      outer.put("phys_cpus", processor.getPhysicalProcessorCount());
      outer.put("log_cpus", processor.getLogicalProcessorCount());
      outer.put("cpu_freq", processorIdentifier.getVendorFreq());
      outer.put("gpu", gpu);
      Runtime rt = Runtime.getRuntime();
      String[] commands = {"nvidia-smi","--query-gpu=name","--format=csv,noheader"};
      Process proc = rt.exec(commands);

      BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

      String s = null;
      String output = new String();
      while ((s = stdInput.readLine()) != null) {
        output+=s;
        output+=";";
      }
      output = output.substring(0, output.length() - 1);
      outer.put("gpu_name",output);
      wr.write(outer.toString());
      wr.close();
    }

    public static void main( String[] args ) throws UIMAException,IOException
    {
      App app = new App();

      String goethe_vor_dem_thor="Vom Eise befreit sind Strom und Bäche\n"
      +"Durch des Frühlings holden, belebenden Blick,\n"
      +"Im Tale grünet Hoffnungsglück;\n"
      +"Der alte Winter, in seiner Schwäche,\n"
      +"Zog sich in rauhe Berge zurück.\n"
      +"Von dort her sendet er, fliehend, nur\n"
      +"Ohnmächtige Schauer körnigen Eises\n"
      +"In Streifen über die grünende Flur.\n"
      +"Aber die Sonne duldet kein Weißes,\n"
      +"Überall regt sich Bildung und Streben,\n"
      +"Alles will sie mit Farben beleben;\n"
      +"Doch an Blumen fehlts im Revier,\n"
      +"Sie nimmt geputzte Menschen dafür.\n"
      +"Kehre dich um, von diesen Höhen\n"
      +"Nach der Stadt zurück zu sehen!\n"
      +"Aus dem hohlen finstern Tor\n"
      +"Dringt ein buntes Gewimmel hervor.\n"
      +"Jeder sonnt sich heute so gern.\n"
      +"Sie feiern die Auferstehung des Herrn,\n"
      +"Denn sie sind selber auferstanden:\n"
      +"Aus niedriger Häuser dumpfen Gemächern,\n"
      +"Aus Handwerks- und Gewerbesbanden,\n"
      +"Aus dem Druck von Giebeln und Dächern,\n"
      +"Aus der Straßen quetschender Enge,\n"
      +"Aus der Kirchen ehrwürdiger Nacht\n"
      +"Sind sie alle ans Licht gebracht.\n"
      +"Sieh nur, sieh! wie behend sich die Menge\n"
      +"Durch die Gärten und Felder zerschlägt,\n"
      +"Wie der Fluß in Breit und Länge\n"
      +"So manchen lustigen Nachen bewegt,\n"
      +"Und, bis zum Sinken überladen,\n"
      +"Entfernt sich dieser letzte Kahn.\n"
      +"Selbst von des Berges fernen Pfaden\n"
      +"Blinken uns farbige Kleider an.\n"
      +"Ich höre schon des Dorfs Getümmel,\n"
      +"Hier ist des Volkes wahrer Himmel,\n"
      +"Zufrieden jauchzet groß und klein:\n"
      +"Hier bin ich Mensch, hier darf ichs sein!";


      String goethe_der_strauss_den_ich_gepfluckt_habe = 	"Der Strauß, den ich gepflücket,"
+"Grüße dich vieltausendmal!"
+"Ich hab mich oft gebücket,"
+"Ach, wohl eintausendmal,"
+"Und ihn ans Herz gedrücket"
+"Wie hunderttausendmal!";

      
      String kafka_aus_dem_grunde = "Aus dem Grunde"
                                    +"der Ermattung\n"
                                    +"steigen wir\n"
                                    +"mit neuen Kräften\n"
                                    +"Dunkle Herren\n"
                                    +"welche warten\n"
                                    +"bis die Kinder\n"
                                    +"sich entkräften\n";
       String das_parfum_patrick_susskind = "Im achtzehnten Jahrhundert lebte in Frankreich ein Mann, der zu den genialsten und abscheulichsten Gestalten dieser an genialen und abscheulichen Gestalten nicht armen Epoche gehörte. Seine Geschichte soll hier erzählt werden. Er hieß Jean-Baptiste Grenouille, und wenn sein Name im Gegensatz zu den Namen anderer genialer Scheusale, wie etwa de Sades, Saint-Justs, Fouchés, Bonapartes usw., heute in Vergessenheit geraten ist, so sicher nicht deshalb, weil Grenouille diesen berühmteren Finstermännern an Selbstüberhebung, Menschenverachtung, Immoralität, kurz an Gottlosigkeit nachgestanden hätte, sondern weil sich sein Genie und sein einziger Ehrgeiz auf ein Gebiet beschränkte, welches in der Geschichte keine Spuren hinterläßt: auf das flüchtige Reich der Gerüche.\n"
+"Zu der Zeit, von der wir reden, herrschte in den Städten ein für uns moderne Menschen kaum vorstellbarer Gestank. Es stanken die Straßen nach Mist, es stanken die Hinterhöfe nach Urin, es stanken die Treppenhäuser nach fauligem Holz und nach Rattendreck, die Küchen nach verdorbenem Kohl und Hammelfett; die ungelüfteten Stuben stanken nach [6] muffigem Staub, die Schlafzimmer nach fettigen Laken, nach feuchten Federbetten und nach dem stechend süßen Duft der Nachttöpfe. Aus den Kaminen stank der Schwefel, aus den Gerbereien stanken die ätzenden Laugen, aus den Schlachthöfen stank das geronnene Blut. Die Menschen stanken nach Schweiß und nach ungewaschenen Kleidern; aus dem Mund stanken sie nach verrotteten Zähnen, aus ihren Mägen nach Zwiebelsaft und an den Körpern, wenn sie nicht mehr ganz jung waren, nach altem Käse und nach saurer Milch und nach Geschwulstkrankheiten. Es stanken die Flüsse, es stanken die Plätze, es stanken die Kirchen, es stank unter den Brücken und in den Palästen. Der Bauer stank wie der Priester, der Handwerksgeselle wie die Meistersfrau, es stank der gesamte Adel, ja sogar der König stank, wie ein Raubtier stank er, und die Königin wie eine alte Ziege, sommers wie winters. Denn der zersetzenden Aktivität der Bakterien war im achtzehnten Jahrhundert noch keine Grenze gesetzt, und so gab es keine menschliche Tätigkeit, keine aufbauende und keine zerstörende, keine Äußerung des aufkeimenden oder verfallenden Lebens, die nicht von Gestank begleitet gewesen wäre.";

      String shakespear_the_tempest = "Safely in harbour \n"
+"Is the king's ship; in the deep nook, where once \n"
+"Thou call'dst me up at midnight to fetch dew \n"
+"From the still-vex'd Bermoothes, there she's hid: \n"
+"The mariners all under hatches stow'd; \n"
+"Who, with a charm join'd to their suffer'd labour, \n"
+"I have left asleep; and for the rest o' the fleet \n"
+"Which I dispersed, they all have met again \n"
+"And are upon the Mediterranean flote, \n"
+"Bound sadly home for Naples, \n"
+"Supposing that they saw the king's ship wreck'd \n"
+"And his great person perish.\n";

      String conan_doyle_sherlock_holmes = "IN THE year 1878 I took my degree of Doctor of Medicine of the University of London, and proceeded to Netley to go through the course prescribed for surgeons in the Army. Having completed my studies there, I was duly attached to the Fifth Northumberland Fusiliers as assistant surgeon. The regiment was stationed in India at the time, and before I could join it, the second Afghan war had broken out. On landing at Bombay, I learned that my corps had advanced through the passes, and was already deep in the enemy's country. I followed, however, with many other officers who were in the same situation as myself, and succeeded in reaching Candahar in safety, where I found my regiment, and at once entered upon my new duties.\n"
+"The campaign brought honours and promotion to many, but for me it had nothing but misfortune and disaster. I was removed from my brigade and attached to the Berkshires, with whom I served at the fatal battle of Maiwand. There I was struck on the shoulder by a Jezail bullet, which shattered the bone and grazed the subclavian artery. I should have fallen into the hands of the murderous Ghazis had it not been for the devotion and courage shown by Murray, my orderly, who threw me across a packhorse, and succeeded in bringing me safely to the British lines.\n";

      int batch_sizes[] = {1,2,4,8,16,32};
      String []texts = {goethe_vor_dem_thor,goethe_der_strauss_den_ich_gepfluckt_habe,kafka_aus_dem_grunde,das_parfum_patrick_susskind,shakespear_the_tempest,conan_doyle_sherlock_holmes};
      String []langu= {"en","en","en","en","en","en"};

      for(int i = 0; i < texts.length; i++) {
        for(int j = 0; j < batch_sizes.length; j++) {
          app.run_test(texts[i], batch_sizes[j],langu[i],false);
        }
      }

      app.write_results(false);
    }
}
