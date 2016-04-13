package romanowski

import java.io.File


object SBTIncrementalCompiler {

  case class Compilation(allSources: Seq[File], output: File, options: Seq[String])

  case class API(source: File, hash: Hash)

  def compile(compilation: Compilation): Unit ={
    var changed = changedSources(compilation)

    while(changed.nonEmpty){
      val oldAPIs = changed.map(currentAPI)
      val newAPIs = doCompile(changed)

      val changedAPIs = diff(oldAPIs, newAPIs)

      changed = changedAPIs.flatMap(allDependecies)
    }
  }

  def changedSources(compilation: Compilation): Seq[File] = {
    def outputChanged = compilation.output != previousCompilation.output
    def optionsChanged = compilation.output != previousCompilation.output

    if(outputChanged || optionsChanged) // Then compile all
      compilation.allSources
    else // Compile only changed files
      compilation.allSources.filter(changedSource)
  }



  private def doCompile(source: Seq[File]): Seq[API] = {
    val listener = new SourceListener

    runScalac(listener)

    updateDependecies(listener.dependencies)

    listener.apis
  }

  class SourceListener{
    private var _apis: Set[API] = Set.empty
    private var _dependencies: Map[File, File] = Map.empty

    def process(unit: CompilationUnit): Unit = {
      val topLevelHashes = unit.topLevelMember.map(hashTopLevel)
      _apis +=  API(unit.source, aggregate(topLevelHashes))

      val allDeps = unit.topLevelMember.flatMap(dependeciesFromTopLevel)

      _dependencies ++= allDeps.map(unit.source -> _)
    }

    def hashTopLevel(topLevel: TopLevelMember): Hash ={
      val signatureHash = hash(topLevel.signature)
      val membersHash = topLevel.members.map(m => hash(m.signature))

      aggregate(signatureHash +: membersHash)
    }

    def dependeciesFromTopLevel(topLevel: TopLevelMember): Set[File] =
      topLevel.members.foldLeft(findDependecies(topLevel.signature)){
        case (current, member) =>
          member.usedInBody.foldLeft(current ++ findDependecies(member.signature)){
            _ ++ findDependecies(_)
          }
      }

    def apis: Seq[API] = _apis.toSeq
    def dependencies: Map[File, File] = _dependencies
  }


  case class CompilationUnit(source: File, topLevelMember: Seq[TopLevelMember])

  case class TopLevelMember(signature: TypeSignature, members: Seq[Member])

  case class Member(signature: TypeSignature, usedInBody: Seq[TypeSignature])

  class TypeSignature

  def findDependecies(signature: TypeSignature): Set[File] = ???

  def updateDependecies(deps: Map[File, File]): Unit = ???

  def runScalac(listner: SourceListener) = ???

  def changedSource(file: File): Boolean = ???

  def hash(v: Any): Hash = ???

  def aggregate(hashes: Seq[Hash]): Hash = ???

  def currentAPI(source: File): API = ???

  def previousCompilation: Compilation = ???

  def diff(oldApis: Seq[API], newApis: Seq[API]): Seq[File] = ???

  def allDependecies(file: File): Seq[File] = ???

  type Hash = Long
}
