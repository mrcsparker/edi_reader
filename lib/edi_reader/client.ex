defmodule EdiReader.Client do
  @doc false
  def get_java_server() do
    java_node = "__edireader__" <> Atom.to_string(Kernel.node())
    {:edireader_java_server, String.to_atom(java_node)}
  end
end
