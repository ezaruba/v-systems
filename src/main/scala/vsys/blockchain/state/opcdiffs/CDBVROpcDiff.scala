package vsys.blockchain.state.opcdiffs

import com.google.common.primitives.Longs
import vsys.blockchain.state._
import vsys.blockchain.transaction.ValidationError
import vsys.blockchain.transaction.ValidationError.{ContractInvalidOPCData, ContractInvalidStateVariable, ContractLocalVariableIndexOutOfRange, ContractStateVariableNotDefined}
import vsys.blockchain.contract.{DataEntry, DataType, ExecutionContext}
import vsys.blockchain.contract.Contract.checkStateVar

import scala.util.{Left, Right, Try}

object CDBVROpcDiff extends OpcDiffer {

  def get(context: ExecutionContext)(stateVar: Array[Byte], dataStack: Seq[DataEntry],
                                     pointer: Byte): Either[ValidationError, Seq[DataEntry]] = {
    if (!checkStateVar(stateVar, DataType(stateVar(1)))) {
      Left(ContractInvalidStateVariable)
    } else if (pointer > dataStack.length || pointer < 0) {
      Left(ContractLocalVariableIndexOutOfRange)
    } else {
      context.state.contractInfo(ByteStr(context.contractId.bytes.arr ++ Array(stateVar(0)))) match {
        case Some(v) => Right(dataStack.patch(pointer, Seq(v), 1))
        case _ => Left(ContractStateVariableNotDefined)
      }
    }
  }

  def mapGet(context: ExecutionContext)(stateMap: Array[Byte], keyValue: DataEntry, dataStack: Seq[DataEntry],
                                        pointer: Byte): Either[ValidationError, Seq[DataEntry]] = {
    if (pointer > dataStack.length || pointer < 0) {
      Left(ContractLocalVariableIndexOutOfRange)
    } else {
      // TODO
      // new validation error
      // for stateMap
      val combinedKey = context.contractId.bytes.arr ++ Array(stateMap(0)) ++ keyValue.bytes
      if (stateMap(0) == 0.toByte) { // amount balance map
        val getVal = context.state.contractNumInfo(ByteStr(combinedKey))
        // for general case, DataType.Amount should get from the stateMap
        Right(dataStack.patch(pointer, Seq(DataEntry(Longs.toByteArray(getVal), DataType.Amount)), 1))
      } else {
        context.state.contractInfo(ByteStr(combinedKey)) match {
          case Some(v) => Right(dataStack.patch(pointer, Seq(v), 1))
          case _ => Left(ContractStateVariableNotDefined)
        }
      }
    }
  }

  def mapGetOrDefault(context: ExecutionContext)(stateMap: Array[Byte], keyValue: DataEntry, dataStack: Seq[DataEntry],
                                                 pointer: Byte): Either[ValidationError, Seq[DataEntry]] = {
    if (pointer > dataStack.length || pointer < 0) {
      Left(ContractLocalVariableIndexOutOfRange)
    } else {
      // TODO
      // for more types
      // for stateMap
      val combinedKey = context.contractId.bytes.arr ++ Array(stateMap(0)) ++ keyValue.bytes
      if (stateMap(0) == 0.toByte) { // amount balance map
        val getVal = context.state.contractNumInfo(ByteStr(combinedKey))
        // for general case, DataType.Amount should get from the stateMap
        Right(dataStack.patch(pointer, Seq(DataEntry(Longs.toByteArray(getVal), DataType.Amount)), 1))
      } else {
        context.state.contractInfo(ByteStr(combinedKey)) match {
          case Some(v) => Right(dataStack.patch(pointer, Seq(v), 1))
          case _ => Right(dataStack.patch(pointer, Seq(DataEntry(Longs.toByteArray(0L), DataType.Timestamp)), 1))
        }
      }
    }
  }

  object CDBVRType extends Enumeration {
    val GetCDBVR = Value(1)
  }

  override def parseBytesDt(context: ExecutionContext)(bytes: Array[Byte], data: Seq[DataEntry]): Either[ValidationError, Seq[DataEntry]] =
    bytes.headOption.flatMap(f => Try(CDBVRType(f)).toOption) match {
      case Some(CDBVRType.GetCDBVR) if bytes.length == 3 && bytes(1) < context.stateVar.length &&
        bytes(1) >= 0 => get(context)(context.stateVar(bytes(1)), data, bytes(2))
      case _ => Left(ContractInvalidOPCData)
    }
}
