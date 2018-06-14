package io.ossim.omar.scdf.regexFilter

import groovy.json.JsonException
import groovy.util.logging.Slf4j
import org.springframework.boot.SpringApplication
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Processor
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.beans.factory.annotation.Value
import groovy.json.JsonSlurper
import groovy.json.JsonException
import java.util.regex.Pattern
import java.util.regex.Matcher

/**
 * Created by ccohee on 6 June 2018
 * Program Description: SCDF filter for determining which messages travel down 
 * ingest queue based on filter criteria Regular expression for filter criteria 
 * can be set in .properties file to control what is ingested into each queue
*/

@SpringBootApplication
@EnableBinding(Processor.class)
@Slf4j
class OmarRegexFilterApplication 
{

    @Value('${filter.path}')
    String filterPath

    @Value('${filter.regex}')
    String filterRegex

    static void main(String[] args) {
        SpringApplication.run OmarRegexFilterApplication, args
    } 

    /** 
    * Method to facilitate filtering of message. The method recieves the 
    * message sent into the Spring Data Stream. After comparing it to a given
    * regex, it decides if to allow it down the SCDF queue or not. 
    */
    @StreamListener(Processor.INPUT)
	@SendTo(Processor.OUTPUT)
    Message<?> filter(final Message<?> message)
    {
        log.debug("Message recieved: ${message} in regex filter") 
       
        boolean result = regexFilter(message)

        if(result){ 
            log.debug(" ***--- SUCCESS ---*** \nMessage meets filter criteria. Ingesting into queue.")
            return message
        }
        else {
            log.debug("***--- FAILURE ---*** \nMessage does not meet filter criteria. Preventing ingest into queue.")
            return null
        }
    }
    
    /** 
    * Method for comparing incoming message against regex filter criteria. 
    * The method inputs the message, converts the payload to a JSON object, 
    * and compares given values against regex. It returns a boolean indicating
    * if the comparison is a success. 
    */
    boolean regexFilter(final Message<?> message)
    {   
        try {   
            def jsonObject = new JsonSlurper().parseText(message.payload)

            ArrayList<String> path = new ArrayList<String>()
            ArrayList<String> regex = new ArrayList<String>()

             // Read in the paths and regex provided (deliminated by commas)
            for(String s : filterPath.split(', |,'))  {   path.add(s)    }
            for(String s : filterRegex.split(', |,')) {   regex.add(s)   }

            // Ensure the same number of regex and paths are provided
            if(path.size() != regex.size()){ 
                log.warn("The number of paths provided does not match the number of regex. Cannot proceed")
                return false
            }

            Pattern pattern
            Matcher matcher

            for(int i = 0; i < path.size(); ++i){
                def value = jsonObject
                
                // Iterate through the path to reach the lowest level for the field
                for(String index : path.get(i).split("\\."))
                    value = value[index]

                pattern = Pattern.compile(regex.get(i))   
                matcher = pattern.matcher(value.toString())  

                if(!matcher.find()){
                    log.debug("JSON value at ${path.get(i)} does not meet filter criteria ${regex.get(i)}")
                    return false
                }
                else
                    log.debug("JSON value at ${path.get(i)} meets regular expression criteria ${regex.get(i)}") 
            }

            return true
        }
        catch (JsonException jsonEx) { log.warn("Message received is not in proper JSON format, skipping\n   Message body: ${message}") }
    }
}
