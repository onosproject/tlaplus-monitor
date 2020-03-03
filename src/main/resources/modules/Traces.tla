------------------------------ MODULE Traces ------------------------------

LOCAL INSTANCE TLC
LOCAL INSTANCE Integers
  (*************************************************************************)
  (* Imports the definitions from the modules, but doesn't export them.    *)
  (*************************************************************************)

LowerBound == 0

UpperBound == 0

Trace(offset) == CHOOSE val : TRUE

============================================================================