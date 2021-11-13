# nats-streaming-server

[Nats streaming server](https://github.com/nats-io/nats-streaming-server) for testing which contains the
original [Nats streaming server](https://github.com/nats-io/nats-streaming-server)

[![Build][build_shield]][build_link]
[![Maintainable][maintainable_shield]][maintainable_link]
[![Coverage][coverage_shield]][coverage_link]
[![Issues][issues_shield]][issues_link]
[![Commit][commit_shield]][commit_link]
[![Dependencies][dependency_shield]][dependency_link]
[![License][license_shield]][license_link]
[![Central][central_shield]][central_link]
[![Tag][tag_shield]][tag_link]
[![Javadoc][javadoc_shield]][javadoc_link]
[![Size][size_shield]][size_shield]
![Label][label_shield]

[build_shield]: https://github.com/YunaBraska/nats-streaming-server/workflows/JAVA_CI/badge.svg
[build_link]: https://github.com/YunaBraska/nats-streaming-server/actions?query=workflow%3AJAVA_CI
[maintainable_shield]: https://img.shields.io/codeclimate/maintainability/YunaBraska/nats-streaming-server?style=flat-square
[maintainable_link]: https://codeclimate.com/github/YunaBraska/nats-streaming-server/maintainability
[coverage_shield]: https://img.shields.io/codeclimate/coverage/YunaBraska/nats-streaming-server?style=flat-square
[coverage_link]: https://codeclimate.com/github/YunaBraska/nats-streaming-server/test_coverage
[issues_shield]: https://img.shields.io/github/issues/YunaBraska/nats-streaming-server?style=flat-square
[issues_link]: https://github.com/YunaBraska/nats-streaming-server/commits/main
[commit_shield]: https://img.shields.io/github/last-commit/YunaBraska/nats-streaming-server?style=flat-square
[commit_link]: https://github.com/YunaBraska/nats-streaming-server/issues
[license_shield]: https://img.shields.io/github/license/YunaBraska/nats-streaming-server?style=flat-square
[license_link]: https://github.com/YunaBraska/nats-streaming-server/blob/main/LICENSE
[dependency_shield]: https://img.shields.io/librariesio/github/YunaBraska/nats-streaming-server?style=flat-square
[dependency_link]: https://libraries.io/github/YunaBraska/nats-streaming-server
[central_shield]: https://img.shields.io/maven-central/v/berlin.yuna/nats-streaming-server?style=flat-square
[central_link]:https://search.maven.org/artifact/berlin.yuna/nats-streaming-server
[tag_shield]: https://img.shields.io/github/v/tag/YunaBraska/nats-streaming-server?style=flat-square
[tag_link]: https://github.com/YunaBraska/nats-streaming-server/releases
[javadoc_shield]: https://javadoc.io/badge2/berlin.yuna/nats-streaming-server/javadoc.svg?style=flat-square
[javadoc_link]: https://javadoc.io/doc/berlin.yuna/nats-streaming-server
[size_shield]: https://img.shields.io/github/repo-size/YunaBraska/nats-streaming-server?style=flat-square
[label_shield]: https://img.shields.io/badge/Yuna-QueenInside-blueviolet?style=flat-square
[gitter_shield]: https://img.shields.io/gitter/room/YunaBraska/nats-streaming-server?style=flat-square
[gitter_link]: https://gitter.im/nats-streaming-server/Lobby

### Family

* Nats **plain Java**
  * [Nats-Server](https://github.com/YunaBraska/nats-server)
  * [Nats-Streaming-Server](https://github.com/YunaBraska/nats-streaming-server)
* Nats for **Spring Boot**
  * [Nats-Server-Embedded](https://github.com/YunaBraska/nats-server-embedded)
  * [Nats-Streaming-Server-Embedded](https://github.com/YunaBraska/nats-streaming-server-embedded)

### Usage

```xml

<dependency>
  <groupId>berlin.yuna</groupId>
  <artifactId>nats-streaming-server</artifactId>
  <version>0.23.3</version>
</dependency>
```

[Get latest version][central_link]

### Configuration priority

1) Custom Arguments
2) Java config
3) Property File (*1)
4) Environment Variables (*1)
5) Default Config

* *1 configs must start with "NATS_" and the additional option
  from [NatsConfig](https://github.com/YunaBraska/nats-server/blob/main/src/main/java/berlin/yuna/natsserver/config/NatsConfig.java))*

### Common methods

#### Getter

| Name                                 | Description                                      |
|--------------------------------------|--------------------------------------------------|
| binaryFile                           | Path to binary file                              |
| downloadUrl                          | Download URL                                     |
| port                                 | port (-1 == not started && random port)          |
| pid                                  | process id (-1 == not started)                   |
| pidFile                              | Path to PID file                                 |
| config                               | Get config map                                   |
| getValue                             | Get resolved config for a key                    |

#### Setter

| Name                                 | Description                                      |
|--------------------------------------|--------------------------------------------------|
| config(key, value)                   | Set specific config value                        |
| config(Map<key, value>)              | Set config map                                   |
| config(key, value...)                | Set config array                                 |

#### Others

| Name                                 | Description                                      |
|--------------------------------------|--------------------------------------------------|
| start                                | Starts the nats server                           |
| start(timeout)                       | Starts the nats server with custom timeout       |
| tryStart()                           | Starts the nats server (mode = RuntimeException) |
| stop()                               | Stops the nats server                            |
| stop(timeout)                        | Stops the nats server with custom timeout        |
| config(Map<key, value>)              | Set config map                                   |
| config(key, value...)                | Set config array                                 |

* All configurations are optional. (see all configs
  here: [NatsStreamingConfig](https://github.com/YunaBraska/nats-streaming-server/blob/main/src/main/java/berlin/yuna/natsserver/config/NatsStreamingConfig.java)

### Example

```java
public class MyNatsTest {

  public static void main(final String[] args) {
    final NatsStreaming nats = new NatsStreaming()
            .source("http://myOwnCachedNatsServerVersion")
            .port(4222) //-1 for a random port
            .config(
                    USER, "yuna",
                    PASS, "braska"
            )
            .start();
    nats.stop();
  }
}
```

```
                                                             .,,.                                                             
                                                              ,/*.                                                            
                                                               *(/.                                                           
                                                               .(#*..                                                         
                                                               ,(#/..                                                         
                                                              ,(#(*..                                                         
                                                             ,/###/,,                                                         
                                                          ..*(#(**(((*                                                        
                                                         ,((#(/. ./##/.                                                       
                                                        ./##/,   ,(##/.                                                       
                                                        ,(((,   ./###*.                                                       
                                                        ,///.  ,(##//.                                                        
                                                         ,**,,/(#(*                                                           
                                                            ,(#(*.                                                            
                                                          ..*((*.                                                             
                                                          ,,((,                                                               
                                                          ..//.                                                               
                                                            .,.                                                               
                                                         .....,.........                                                      
                                            ..,**///(((((##############(((((((//*,..                                          
                                       .*//((#######################################((/*,.                                    
                                    ,/(###################################################/*..                                
                                .,,(########################################################((/.                              
                                ,((###########################################################(/.                             
                              .(#################################################################*                            
                             .(#.###############################################################*                           
                            ./#....###########################################################...,                          
                            .(#.....########################################################.....*                          
                            ,#.........##################################################.........#/.                         
                            *#...##.........##########################################........#...##(((//*,.                  
                            ,#..####...#............##########################..........#...####..........##/..               
                            ,#..####..###..............................................###..####...........##**               
                            .(#.#####.###....##..................................##...####.#####..#/,,,,/##..((.              
                             ,#..####..####..###....###..................####...###...###..####..#/.    .(#..((.              
                             ./#.#####.#####.####...####................#####..####.#####.#####.,    ./#..#//.              
                              ,#.#####.#####.#####..####................#####.####..##########..#/.    ,##.,,               
                               *#..######################..............#######################.#(.  .//#..##*                 
                                *##.#####################..............#####################..#(,.,/###..#(,                  
                                 **#######################............#####################...#####....##*.                   
                                   ,(#.#####################.........####################.........###//,                      
                                    *########################......######################..#######(/,..                       
                                     ,(#######################....########.############..#/,....                              
                                      ./###############.#####......#####.#############.##/**,,,.                              
                                        ,((#.###########..###......##...###########...#(/*********,.                          
                                         ,,(#.###########..............###########..###/************,..                       
                                            *(#.############.........###########..##//******************,.                    
                                              ,/#############......###########.##(/***********************,.                  
                                                .*((########.........#####.###/,...,,,,,,*******************,..               
                                                  ,,/########......#####.##((*.         ....,,,,*************,,.              
                                                     .,(##.............##(*.....,,,,,,,,,,,,,,,,,**************,              
                                                        .*###........##(/***********************************,..               
                                                            *(#...###/************************************,.                  
                                                             *(#.##//************************************,.                   
                                                              ./(*.,,********************************,,.                      
                                                                       .,************************,..                          
                                                                           ...,,,,******,,,,...                           
```
