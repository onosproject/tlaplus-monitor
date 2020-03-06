------------------------------ MODULE Traces ------------------------------

LOCAL INSTANCE TLC
LOCAL INSTANCE Integers
  (*************************************************************************)
  (* Imports the definitions from the modules, but doesn't export them.    *)
  (*************************************************************************)

Trace(offset) == CHOOSE val : TRUE

UpperBound == TRUE

============================================================================