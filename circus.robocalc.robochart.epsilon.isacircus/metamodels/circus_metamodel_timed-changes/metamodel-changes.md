# Circus Time 版元模型的全部改动，以及对生成的 Circus 模型的影响

比较对象：公开仓库 `UoY-RoboStar/robochart-isacircus-gen` 的 `2fba5ea`（2026-10-09）中的 `circus_metamodel.ecore`，和 `isa-gen-ext/metamodels/circus_metamodel_timed.ecore`。
合计：**+93 行，−4 行**。精确的逐行改动见两个补丁：

- `circus-metamodel-fix.patch`：原元模型 → Circus 版（下面的 A 部分，+9 / −4）
- `metamodel-timed.patch`：Circus 版 → Circus Time 版（下面的 B 部分，+84）

**预期结论：生成的 Circus 模型和 `.thy` 都不受影响**。理由逐项写在下表中，第 3 节是在 Ubuntu 上核对的方法。

## A. 未加时间的部分（Circus 部分修正，两个版本都有）

| # | 改动 | 原来 | 现在 | 现有 M2M/M2T 是否用到 | 对生成模型的影响 |
|---|---|---|---|---|---|
| A1 | `CircusAnn` 的父类 | `ActionList` | `Ann` | 没有用到（它的 8 个子类也没有用到） | 无。`ActionList` 和 `Ann` 都没有字段，`CircusAnn` 的字段不变 |
| A2 | `ParenAnn` 的父类 | `Term` | `Ann` | 没有用到 | 无。`Ann` 继承 `Term`，而且两者都没有字段 |
| A3 | `CircusCommunicationList` 的父类 | `Term` | `CommunicationList` | **用到了**：RC2Z.eol 第 5255–5283 行 `new Z!CircusCommunicationList` 和 `.communications`；各模板中的 `communicationList.communications` | 无。`CommunicationList` 没有字段，所以 `communications` 字段不变；三个引用它的地方，类型都是 `CircusCommunicationList` 本身。只有按父类查询的代码（`Z!CommunicationList.all()`、`isKindOf(Z!CommunicationList)`）结果才会变，而公开版中没有这样的代码 |
| A4 | 删除 `ExtChoiceProcessImpl` | 一个空类，继承 `Process2` | 已删除 | 没有用到 | 无。它是 CZT 的实现类，和 `ExtChoiceProcess` 重复。如果某个**存盘的** XMI 模型中出现了这个类，加载时会失败（见第 3 节第 2 步） |
| A5 | `Term` 新增 `anns` | 没有 | containment、多值、类型为 `Ann` | 没有用到（没有代码往里放东西） | 无。现在每个对象都有一个**空的** `anns` 列表；空列表不会被序列化，也不会出现在 `eContents()` 中。只有以后往里放了注解，`eContents()`/`eAllContents()` 才会返回注解对象 |

## B. Circus Time 部分（只有 Circus Time 版有）

**纯新增**：15 个新类，原有的类一个都没有改。依据是 CZT 的 Circus Time AST（`net.sourceforge.czt.circustime.ast`，API 文档：<https://czt.sourceforge.net/corejava/corejava-circustime/apidocs/index.html>）。

| 类 | 继承 | 新增的字段 |
|---|---|---|
| `WaitAction` | `BasicAction` | `expr: Expr` |
| `ActionTime1`（抽象） | `Action1` | `expr: Expr` |
| `WaitExprAction` | `ActionTime1` | `name: ZName` |
| `TimeEndByAction` | `ActionTime1` | — |
| `TimeStartByAction` | `ActionTime1` | — |
| `ActionTime2`（抽象） | `Action2` | `expr: Expr` |
| `TimeoutAction` | `ActionTime2` | — |
| `TimedinterruptAction` | `ActionTime2` | — |
| `PrefixingTimeAction` | `PrefixingAction` | `channelElapsedTime: ZName`、`expr: Expr` |
| `ProcessTime1`（抽象） | `Process1` | `expr: Expr` |
| `TimeEndByProcess` | `ProcessTime1` | — |
| `TimeStartByProcess` | `ProcessTime1` | — |
| `ProcessTime2`（抽象） | `Process2` | `expr: Expr` |
| `TimeoutProcess` | `ProcessTime2` | — |
| `TimedinterruptProcess` | `ProcessTime2` | — |

所有引用都是 containment，每个类都带有说明来源的 documentation 注解。

对生成模型的影响：**无**。现有的 M2M 不会创建这些类的实例，模板也不会读取它们。不过，如果现有代码中有对 `Z!BasicAction`、`Z!Action1`、`Z!Action2`、`Z!PrefixingAction`、`Z!Process1`、`Z!Process2` 做 `.all()` 的地方，新类也属于这些父类。由于新类没有实例，结果不会变。

## 3. 在 Ubuntu 上核对的方法

1. **搜索可能受影响的用法**（在仓库根目录下执行）：

   ```bash
   grep -rnE 'Z!(CommunicationList|Ann|ActionList|Term)\b|ExtChoiceProcessImpl|CircusAnn|ParenAnn|eAllContents|eAllStructuralFeatures|\.anns\b' \
     circus.robocalc.robochart.epsilon.isacircus/erules circus.robocalc.robochart.epsilon.isacircus/src
   ```

   在公开版中，结果里只有 `CircusCommunicationList` 的创建和 `.communications`，这些都不受影响。如果你的 Ubuntu 版本中出现了按父类查询、`eAllContents`，或者用到 `ExtChoiceProcessImpl` 的代码，就要逐一看一下。

2. **检查有没有存盘的 Circus 模型（XMI）**：如果有，要用新的元模型重新加载一次，看是否报错（主要是 A4）。

   ```bash
   grep -rl 'ExtChoiceProcessImpl' --include='*.xmi' --include='*.model' . 2>/dev/null
   ```

3. **回归比较**（最可靠的一步）：对同一组例子（例如 `robot_runningexample.rct`、TurtleBot MakePlan，以及其他手头的模型），分别用**原元模型**、**Circus 版**、**Circus Time 版**各运行一次 Compile，然后比较生成的 `.thy`：

   ```bash
   diff -r <原元模型生成的目录> <新元模型生成的目录>
   ```

   除了文件名里的时间戳，内容应该**完全相同**。如果文件名不同，可以先把时间戳去掉再比较。

4. **可选**：如果插件能把 M2M 生成的中间 Circus 模型存成 XMI，比较新旧两份 XMI，会比比较 `.thy` 更直接。

5. 在 Eclipse 中用 Sample Ecore Model Editor 打开 `.ecore`，右键根节点选择 **Validate**，确认没有错误。

## 4. 放入生成器仓库时的改动：nsURI 和 nsPrefix（2026-11-06）

放入 `robochart-isacircus-gen`（分支 `holcsp-fixes`）时，文件放在 `metamodels/circus_metamodel_timed.ecore`，与原有的 `metamodels/circus_metamodel.ecore` 并列。原文件不变。

对上面两个补丁的结果，只另外改了根元素的两个属性：

| 属性 | 原来（与 `circus_metamodel.ecore` 相同） | 现在 |
|---|---|---|
| `nsURI` | `http://www.robocalc.circus/Circus` | `http://www.robocalc.circus/Circus/timed` |
| `nsPrefix` | `circus` | `circustimed` |
| `name` | `circus` | `circus`（不变） |

原因：EMF 的全局 EPackage 注册表以 nsURI 为键。如果两个元模型用同一个 nsURI，后注册的会覆盖先注册的，插件中 `EPackage.Registry.INSTANCE.containsKey("http://www.robocalc.circus/Circus")` 的判断也会把错误的那个当成已注册。改用独立的 nsURI 后，以后不带时间和带时间的元模型可以同时注册而不冲突。nsPrefix 相应改为可区分的名字，这样在 XMI 中也能看出用的是哪一个。包名 `name` 没有改，因为注册不需要，改它会影响以后可能生成的 Java 包名，以及 Epsilon 按包名的查找。

内部引用都是 `#//…` 形式，不含 nsURI（只有 Ecore 自身的数据类型引用 `http://www.eclipse.org/emf/2002/Ecore#…`），所以改 nsURI 不影响文件中的任何引用。

目前这个元模型**不使用**：插件只按固定路径加载并注册 `metamodels/circus_metamodel.ecore`（`CompileToIsaCircusHandler`），模型的 `setMetamodelUri` 固定为 `http://www.robocalc.circus/Circus`；`plugin.xml` 中没有 EMF 的 `generated_package` 扩展。由于 `build.properties` 包含整个 `metamodels/` 目录，这个文件和本目录会被打进插件 jar，但不会被加载。

两个补丁中的路径指向 `metamodels/circus_metamodel.ecore`（相对于 2fba5ea 的原文件）。依次应用到原文件，得到的是改 nsURI/nsPrefix 之前的版本，即 `~/Downloads/circus_metamodel_timed.ecore`（已核对，逐字节相同）。
