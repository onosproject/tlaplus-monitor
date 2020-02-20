----------------------------- MODULE JsonUtils -----------------------------

LOCAL INSTANCE TLC
LOCAL INSTANCE Integers
  (*************************************************************************)
  (* Imports the definitions from the modules, but doesn't export them.    *)
  (*************************************************************************)

JsonSerialize(absoluteFilename, value) == TRUE

JsonDeserialize(absoluteFilename) == CHOOSE val : TRUE

============================================================================