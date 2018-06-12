# Omar SCDF Regex Filter
Omar SCDF Regex Filter acts as a filter within a Spring Cloud Stream. The filter either allows or prevents a message from continuing down the stream based on some given criteria. Filter criterias require a JSON path and a regular expression. 

## Assumptions ##
- The message passed into the filter is in JSON format.
- The JSON paths to the filter criteria values exist and are accurate.
- The number of JSON paths and the number of regular expressions provided are the same.

## Options ## 
Omar SCDF Regex Filter has the following properties that can be specified during deployment:
<dl>
  <dt>filter.path</dt>
  <dd>Path to JSON value for which to evaluate regular expression criteria. Several paths that are specified should be comma    delimited <strong>(String, default value: empty)</strong></dd>
  <dd><strong>Example: --filter.path = Message.uRL, Message.xAxisDimension</strong></dd>
</dl>
<dl>
  <dt>filter.regex</dt>
  <dd>Regular expression to evaluate against value at specified path. Several regular expression that are given should be comma delimited <strong>(String, default value: empty)</strong></dd>
  <dd><strong>Example: --filter.regex = [^\\s]+(\.(nif|png)), \d+</strong></dd>
</dl>
    
