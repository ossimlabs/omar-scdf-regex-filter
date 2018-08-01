# Omar SCDF Regex Filter
Omar SCDF Regex Filter acts as a filter within a Spring Cloud Stream. The filter either allows or prevents a message from continuing down the stream based on some given criteria. Filter criterias require a JSON path, a regular expression, and SQS queues to send the message if the regex matches. 

## JSON Selector ##
Filter specifications are passed in through the properties variable 'selector'. Input is expected to be a JSON array, with each element containing the pair of regex, paths, and queues to be evaluated. For each element in the array, the path is evaluated with the regex, and the message is sent to the specified if the comparison is a success.

Example:
```
{"selector":[
  {
    "queue": "queue1",
    "path": "path1, path2"
    "regex": "regex1"
  },
  {
    "queue": "queue2",
    "path": "path3",
    "regex": "regex2"
  }
  ]
}
```
It is imperative for the JSON array to follow this format. The JSON array must be named "selector", and each key in the array must be named "queue", "path", and "regex".

Multiple paths may be specified in each array element. In this case, if any of the paths match with the regex provided, a message will be sent to the designated queue.

Only one regex may be provided for each array element. **Note:** Be mindful - because the Selector JSON array is stringified, all special characters in regular expressions need to be escaped properly.

Multiple queues may be specified in each array element. In this case, if the regex comparison is a success, a message will be sent to each queue. 

If none of the regex comparisons match from the JSON array, the message is sent to the default SQS queue as a last resort.

## Properties ## 
Omar SCDF Regex Filter has the following properties that can be specified during deployment:

| Property                          | Description                       | Type      | Example                              |
|:----------------------------------|:----------------------------------|:----------|:--------------------------------------
|spring.cloud.stream.bindings.input.destination|The incoming channel for this app to listen and receive messages from |string|sqs-message|
|spring.cloud.stream.bindings.output.desintation|The outgoing channel to send messages to after being processed in this app |string|files-extracted|
|default.queue|Default SQS queue to send message if no regex comparison matches|string|default-queue|
|selector|Stringify JSON array with groupings of path, regex, and queue that are to be evaluated together|string|{"selector":[{"queue": "queueA", "path": "Message.uRL", "regex": ".*"}]}|

**Important:** Properties can also be configured during deployment through Openshift (with more reliability than through the Spring Cloud Data Flow dashboard). After deploying the stream, navigate to the corresponding Openshift project and click on 'Deployment Config' for the filter pod. Proceed to 'Environment' -> 'Add Environment Variable', and add FILTER_PATH and FILTER_REGEX. Input the desired values in the corresponding fields. 
