package io.ossim.omar.scdf.regexfilter

import groovy.util.logging.Slf4j
import org.springframework.boot.SpringApplication
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

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.amazonaws.services.sqs.model.SendMessageRequest
import com.amazonaws.AmazonServiceException

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
    @Value('${selector}')
    String selector

    @Value('${default.queue}')
    String defaultQueue

    String filterPath
    String filterRegex
    ArrayList<String> sqsQueue

    /** 
     * The main entry point of the SCDF Regex Filter application. 
     * @param arg
     */
    static final void main(String[] args) {
        SpringApplication.run OmarRegexFilterApplication, args
    } 

    /** 
    * Method to facilitate filtering of message. The method recieves the 
    * message sent into the Spring Data Stream. After comparing it to a given
    * regex, it decides if to allow it down the SCDF queue or not.     
    * 
    * @param message body input from the sqs source
    * @return message containing JSON information
    */
    @StreamListener(Processor.INPUT)
    @SendTo(Processor.OUTPUT)
    final Message<?> filter(final Message<?> message)
    {
        log.debug("Message recieved: ${message.payload} in regex filter") 
        
        AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient()
        boolean useDefault = true   // Varable to track if any of the regexs are successful
        def jsonSelector

        if(selector){
            try {
                jsonSelector = new JsonSlurper().parseText(selector)
            
                jsonSelector.selector.each { property->
                    filterPath = property.path
                    filterRegex = property.regex
                    sqsQueue = property.queue?.split(',').collect{ it.trim() }

                    log.debug("Comparing regex [${filterRegex}] on path(s) [${filterPath}] with destination queue(s) ${sqsQueue}")

                    boolean result = regexFilter(message)

                    if(result){ 
                        log.debug("SUCCESS: Message meets filter criteria.")

                        try {
                            sqsQueue.each { queue->
                                // Send message to specified SQS queue
                                String sqsUrl = sqs.getQueueUrl(queue).getQueueUrl()
                                SendMessageRequest sqsMessage = new SendMessageRequest(sqsUrl, message.payload)
                                sqs.sendMessage(sqsMessage)

                                log.debug("Successfully sent message to SQS queue: [${queue}]")
                                useDefault = false
                            }
                        } 
                        catch(AmazonServiceException e)
                        {
                            log.error("Error sending message to SQS [${sqsQueue}] - ${e} ")
                        }
                    }
                    else 
                        log.debug("FAILURE: Message does not meet filter criteria. Preventing ingest to queue [${sqsQueue}].")
                }
            } catch(JsonException e)
            {
                log.error("Selector provided is not in proper JSON format ${e}")
            }

            // Send message to default SQS queue if all other conditions fail
            if(useDefault){
                try {
                    log.debug("DEFAULT: Message does not meet any regex provided. Sending to default SQS Queue: [${defaultQueue}]")
                    
                    String defaultUrl = sqs.getQueueUrl(defaultQueue).getQueueUrl()
                    SendMessageRequest sqsMessage = new SendMessageRequest(defaultUrl, message.payload)
                    sqs.sendMessage(sqsMessage)
                } 
                catch(AmazonServiceException e)
                {
                    log.error("Error sending message to SQS [${sqsQueue}] - ${e} ")
                }
            }

            return message
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
        boolean result = false 

        ArrayList<String> paths = filterPath?.split(',').collect { it.trim() }
        String regex = filterRegex

        // Ensure that regex and paths are provided
        if(!(paths && regex)){ 
            log.error("Filter path or filter regex must be supplied. Cannot proceed.")
            return result
        }

        def jsonObject
        Matcher matcher    
        Pattern pattern = Pattern.compile(regex)
        
        try { 
            jsonObject = new JsonSlurper().parseText(message.payload) 
        }
        catch(JsonException e) { 
            log.error("Message is not in proper JSON format: ${e}")
            return result
        }

        paths.each { path->                
            String[] pathArray = path.split("\\.")
            String leaf = getLeafValue(jsonObject, pathArray, 0)
            // Ensure JSON path is valid
            try {
                matcher = pattern.matcher(leaf)  

                if(matcher.find())
                    result = true
            }
            catch(e){
                log.error("Invalid JSON message or invalid JSON path. ${e}")
            }
        }

        result
    }

    /**
    * Recursive method for traversing down JSON using the path provided to reach
    * the lowermost leaf value needed for regex comparison. It inputs a JSON object 
    * to traverse down, a path array to indicate which JSON key to focus on, and
    * an index to correlate with the path array. The method returns a string of
    * the lowest most leaf reached from the path.
    **/
    String getLeafValue(def jsonObject, def pathArray, int index){
        String result

        if(jsonObject){
            def value = jsonObject."${pathArray[index]}"

            // Base case
            if(index+1 >= pathArray.size()){
                result = value 
            }
            else if(isInstanceOfString(value)){
                def newJson = new JsonSlurper().parseText(value)
                result = getLeafValue(newJson, pathArray, index+1) 
            }
            else 
                result = getLeafValue(value, pathArray, index+1)
        }

        result
    }

    /**
    * Private function for checking if an object is of type string. It inputs
    * a value (usually a JSON object or a string) and checks its class instance.
    * The method returns a boolean indicating if it is of type string.
    **/
    private boolean isInstanceOfString(def value){
        boolean result = false

        if(value instanceof String || value instanceof GString)
            result = true
                
        result
    }
}
