package control;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import java.util.logging.Logger;
import model.Sound;

/**
 *
 * @author Philipp Schulz
 */
public class VoiceRecognition
{
    private Configuration configuration;
    private LiveSpeechRecognizer recognizer;
    
    private boolean isRunning;
    
    private final String USER_DIR = "user.dir";
    //Strings for Logger
    private final String LOGGER_DEFAULT_CONFIG = "default.config";
    private final String LOGGER_CON_FILE = "java.util.logging.config.file";
    private final String LOGGER_PROPERTY1 = "java.util.logging.config.file";
    private final String LOGGER_PROPERTY2 = "ignoreAllSphinx4LoggingOutput";

    //Constants required for CMU Sphinx4
    private final String ACOUSTIC_MODEL_PATH = "resource:/edu/cmu/sphinx/models/en-us/en-us";
    private final String DICTIONARY_PATH = "resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict";
    private final String LANGUAGE_MODEL_PATH = "resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin";
    private final String GRAMMAR_NAME = "CMUSphinx";
    private final String GRAMMAR_PATH = "file:///home/pi/source/";
    private final String FILE = "file:///";
    private final String WINDOWS_GRAMMAR_PATH = "/source/";

    private final String ERROR_CREATE_RECOGNIZER = "could not create recognizer";
    
    /**
     * Instantiates the VoiceRecognition class
     * @author Philipp Schulz
     * @param sound Instance of the Sound class
     */
    public VoiceRecognition(Sound sound)
    {
        this.isRunning = false;
        //modify logger to stop spam in console
        Logger cmRootLogger = Logger.getLogger(LOGGER_DEFAULT_CONFIG);
        cmRootLogger.setLevel(java.util.logging.Level.OFF);
        String conFile = System.getProperty(LOGGER_CON_FILE);
        if(conFile == null)
        {
            System.setProperty(LOGGER_PROPERTY1, LOGGER_PROPERTY2);
        }
        configuration = new Configuration();
        configuration.setAcousticModelPath(ACOUSTIC_MODEL_PATH);
        configuration.setDictionaryPath(DICTIONARY_PATH);
        configuration.setLanguageModelPath(LANGUAGE_MODEL_PATH);
        configuration.setUseGrammar(true);
        configuration.setGrammarName(GRAMMAR_NAME);
        if(!sound.isWindows)
        {
            configuration.setGrammarPath(GRAMMAR_PATH);
        }
        else
        {
            configuration.setGrammarPath(FILE+System.getProperty(USER_DIR)+WINDOWS_GRAMMAR_PATH);
        }
        try
        {
            recognizer = new LiveSpeechRecognizer(configuration);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println(ERROR_CREATE_RECOGNIZER);
        }
    }
    
    /**
     * Listens to voice input and returns the spoken String
     * @author Philipp Schulz
     * @return Result of the voice input
     */
    public String recognize()
    {
        if(isRunning)
        {
            SpeechResult result = recognizer.getResult();
            return result.getResult().getBestFinalResultNoFiller();
        }
        else
        {
            return null;
        }
    }
    
    /**
     * Turns on recognition
     * @author Philipp Schulz
     */
    public void turnOnRecognition()
    {
        if(!isRunning)
        {
            isRunning=true;
            recognizer.startRecognition(true);
        }
    }
    
    /**
     * Turns off recognition
     * @author Philipp Schulz
     */
    public void turnOffRecognition()
    {
        if(isRunning)
        {
            isRunning=false;
            recognizer.stopRecognition();
        }
    }
    
}
