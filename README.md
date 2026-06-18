# SideScrollingGame

Java Swingで作成した横スクロールゲームです。

このプロジェクトには、通常版、難しめ版、オートスクロール・ランダム生成版の3種類があります。

## ゲームの種類

| ファイル | 内容 |
| --- | --- |
| `src/SideScrollingGame.java` | 通常版です。`map.txt` を読み込んでステージを表示します。 |
| `src/SideScrollingGameHard.java` | 難しめ版です。`map_hard.txt` を読み込み、敵の速度や配置が通常版より難しくなっています。 |
| `src/SideScrollingGameAuto.java` | オートスクロール版です。ステージをランダムに生成しながら自動で右へ進みます。 |

## Eclipseで実行する方法

1. Eclipseで `SideScrollingGame` プロジェクトを開きます。
2. `src` フォルダを開きます。
3. 実行したいJavaファイルを開きます。
4. 右クリックして `実行` → `Java アプリケーション` を選びます。

実行するJavaファイルによって、起動するゲームが変わります。

| 実行するファイル | 起動するゲーム |
| --- | --- |
| `SideScrollingGame.java` | 通常版 |
| `SideScrollingGameHard.java` | 難しめ版 |
| `SideScrollingGameAuto.java` | オートスクロール・ランダム生成版 |

## jarで実行する方法

jarファイルをダブルクリックして起動できます。

ダブルクリックで動かない場合は、PowerShellでプロジェクトフォルダを開いてから次のように実行します。

```powershell
C:\pleiades\2023-09\java\17\bin\java.exe -jar SideScrollingGameAuto.jar
```

jarごとの内容は次の通りです。

| ファイル | 内容 |
| --- | --- |
| `SideScrollingGame.jar` | 通常版 |
| `SideScrollingGameHard.jar` | 難しめ版 |
| `SideScrollingGameAuto.jar` | オートスクロール・ランダム生成版 |

## 操作方法

| キー | 操作 |
| --- | --- |
| 左矢印キー | 左へ移動 |
| 右矢印キー | 右へ移動 |
| スペースキー | ジャンプ |

敵に当たるとゲームオーバーです。

## ステージファイル

| ファイル | 内容 |
| --- | --- |
| `map.txt` | 通常版で使うステージデータです。 |
| `map_hard.txt` | 難しめ版で使うステージデータです。 |

ステージデータでは、文字によって地形やキャラクターを表しています。

| 文字 | 意味 |
| --- | --- |
| `S` | プレイヤーのスタート位置 |
| `G` | ゴール |
| `P` | 足場 |
| `F` | 地面 |
| `E` | 敵 |
| `.` | 何もない場所 |

`SideScrollingGameAuto.java` はステージを自動生成するため、マップファイルは使いません。

## その他のファイル

| ファイル・フォルダ | 内容 |
| --- | --- |
| `bin/` | コンパイルされた `.class` ファイルが入っています。 |
| `.classpath` | EclipseのJava設定ファイルです。 |
| `.project` | Eclipseのプロジェクト設定ファイルです。 |
| `.settings/` | Eclipseの設定フォルダです。 |
| `SideScrollingGame.exe` | 通常版を起動するための実行ファイルです。 |
| `SideScrollingGame.xml` | exe作成に使うLaunch4j設定ファイルです。 |
| `launch4j.log` | Launch4jのログファイルです。 |
| `SideScrollingGame_screen_capture.png` | ゲーム画面のスクリーンショットです。 |

## 開発メモ

- 元の通常版は `SideScrollingGame.java` です。
- 難しめ版は、通常版と見分けやすいように `Hard Mode` と表示されます。
- オートスクロール版は、通常版や難しめ版とは別の `SideScrollingGameAuto.java` として追加されています。
- 新しく変更する場合は、まず `src` 内のJavaファイルを編集し、Eclipseで実行確認してください。
