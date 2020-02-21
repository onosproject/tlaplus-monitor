---------------------------- MODULE KafkaUtils ----------------------------

LOCAL INSTANCE TLC
LOCAL INSTANCE Integers
  (*************************************************************************)
  (* Imports the definitions from the modules, but doesn't export them.    *)
  (*************************************************************************)

KafkaProduce(topic, value) == TRUE

KafkaConsume(topic) == CHOOSE val : TRUE

============================================================================