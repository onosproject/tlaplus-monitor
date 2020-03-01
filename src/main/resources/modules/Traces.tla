------------------------------ MODULE Traces ------------------------------

LOCAL INSTANCE TLC
LOCAL INSTANCE Integers
  (*************************************************************************)
  (* Imports the definitions from the modules, but doesn't export them.    *)
  (*************************************************************************)

BeginWindow == CHOOSE val : TRUE

NextWindow == CHOOSE val : TRUE

EndWindow == CHOOSE val : TRUE

============================================================================