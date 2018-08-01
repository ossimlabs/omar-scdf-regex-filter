# Omar SCDF Regex Filter
Omar SCDF Regex Filter acts as a filter within a Spring Cloud Stream. The filter either allows or prevents a message from continuing down the stream based on some given criteria. Filter criterias require a JSON path, a regular expression, and SQS queues to send the message if the regex matches. 

# JSON Selector
Filter specifications are passed in through the properties variable 'selector'. Input is expected to be a JSON array, with each element containing the pair of regex, paths, and queues to be evaluated. For each element in the array, the path is evaluated with the regex, and the message is sent to the specified if the comparison is a success.
<dd><i> Example:
{"selector":[
  {
    "queue": "queue1",
    "path": "path1, paht2"
    "regex": "regex1"
  },
  {
    "queue": "queue2",
    "path": "path3",
    "regex": "regex2"
  }
  ]
}
</dd></i>
It is imperative for the JSON array to follow this format. The JSON array must be named "selector", and each key in the array must be named "queue", "path", and "regex".

## Assumptions ##
- The message passed into the filter is in proper JSON format or stringify JSON format.
- The JSON paths to the filter criteria values exist and are case sensitive.
- The SQS queue to forward messages must exist.

## Options ## 
Omar SCDF Regex Filter has the following properties that can be specified during deployment:
<dl>
  <dt>default.queue</dt>
  <dd>Default SQS queue to send messages if no other regex-path pairs match.</dd>
  <dd><strong>(String, default value: default-queue)</strong></dd>
  <dd><i>Example: --default-queue = regexfilter-default-queue</i></dd>
</dl>
<dl>
  <dt>selector</dt>
  <dd>JSON array that specifies the pair of regex, JSON paths, and SQS queue that are to be evaluated together. Multiple JSON paths and SQS queues can be specified in each element.</dd>
  <dd><strong>(String, default value: {"selector":[{"queue": "first-queue", "path": "Message.uRL", "regex": ".*(\\.ntf|nitf)"}, {"queue": "second-queue", "path": "Message.uRL", "regex": ".*(\\.jpeg)"}]} </strong></dd>
  <dd><i>Example: --selector={"selector":[{"queue": "first-queue", "path": "Message.uRL, Message.abstract", "regex": "\\d"}]} </i></dd>
</dl>
<dl>
  <dt>spring.cloud.stream.bindings.input.destination</dt>
  <dd>The message input channel. <strong>(String, default value: sqs-messages)</strong></dd> 
</dl>
<dl>
  <dt>spring.cloud.stream.bindings.output.destination</dt>
  <dd>The message output channel. <strong>(String, default value: files-extracted)</strong></dd> 
</dl>
<strong>Important:</strong> Properties can also be configured during deployment through Openshift (with more reliability than through the Spring Cloud Data Flow dashboard). After deploying the stream, navigate to the corresponding Openshift project and click on 'Deployment Config' for the filter pod. Proceed to 'Environment' -> 'Add Environment Variable', and add FILTER_PATH and FILTER_REGEX. Input the desired values in the corresponding fields. 
