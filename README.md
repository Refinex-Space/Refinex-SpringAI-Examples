# Refinex-SpringAI-Examples

[![Java](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.x-brightgreen?logo=spring)](https://spring.io/projects/spring-ai)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **Spring AI 系列博客配套参考代码** — 一系列将大型语言模型（LLM）集成到 Spring 应用中的实战示例。
>
> **Reference code for the Spring AI blog series** — Practical examples integrating Large Language Models (LLMs) into Spring applications.

---

## 目录 / Table of Contents

- [项目简介 / Introduction](#项目简介--introduction)
- [技术栈 / Tech Stack](#技术栈--tech-stack)
- [示例模块 / Example Modules](#示例模块--example-modules)
- [前置条件 / Prerequisites](#前置条件--prerequisites)
- [快速开始 / Quick Start](#快速开始--quick-start)
- [配置说明 / Configuration](#配置说明--configuration)
- [项目结构 / Project Structure](#项目结构--project-structure)
- [贡献指南 / Contributing](#贡献指南--contributing)
- [许可证 / License](#许可证--license)

---

## 项目简介 / Introduction

**中文**

本项目是《Spring AI 实战》系列博客的配套代码仓库，旨在帮助 Java 开发者快速掌握如何使用 Spring AI 框架将各类大型语言模型（LLM）无缝集成到 Spring Boot 应用中。每个示例模块均对应博客中的一个具体主题，代码简洁、注释详尽，适合入门学习与工程实践参考。

**English**

This repository contains the companion code for the *Spring AI in Practice* blog series. It is designed to help Java developers quickly learn how to integrate various Large Language Models (LLMs) into Spring Boot applications using the Spring AI framework. Each example module corresponds to a specific blog topic, with clean code and detailed comments suitable for both beginners and experienced engineers.

---

## 技术栈 / Tech Stack

| 技术 / Technology | 版本 / Version | 说明 / Notes |
|---|---|---|
| Java | 17+ | LTS 版本推荐 / LTS recommended |
| Spring Boot | 3.x | 基础框架 / Base framework |
| Spring AI | 1.x | AI 集成框架 / AI integration framework |
| Maven | 3.8+ | 构建工具 / Build tool |
| OpenAI API | — | 默认 LLM 提供商 / Default LLM provider |
| Ollama | Latest | 本地模型支持 / Local model support |

---

## 示例模块 / Example Modules

以下为本系列计划涵盖或已覆盖的示例主题：

| 模块 / Module | 主题 / Topic | 状态 / Status |
|---|---|---|
| `chat-basic` | 基础对话 / Basic Chat Completion | 🚧 即将推出 / Coming Soon |
| `chat-streaming` | 流式对话 / Streaming Chat | 🚧 即将推出 / Coming Soon |
| `prompt-template` | 提示词模板 / Prompt Templates | 🚧 即将推出 / Coming Soon |
| `function-calling` | 函数调用 / Function Calling | 🚧 即将推出 / Coming Soon |
| `rag-basic` | 基础 RAG / Basic Retrieval-Augmented Generation | 🚧 即将推出 / Coming Soon |
| `vector-store` | 向量数据库 / Vector Store Integration | 🚧 即将推出 / Coming Soon |
| `embeddings` | 文本嵌入 / Text Embeddings | 🚧 即将推出 / Coming Soon |
| `image-generation` | 图片生成 / Image Generation | 🚧 即将推出 / Coming Soon |
| `multimodal` | 多模态输入 / Multimodal Input | 🚧 即将推出 / Coming Soon |
| `ollama-local` | Ollama 本地模型 / Ollama Local Model | 🚧 即将推出 / Coming Soon |

---

## 前置条件 / Prerequisites

在运行任何示例之前，请确保您已安装以下工具：

Before running any example, make sure you have the following installed:

- **JDK 17+** — [下载 / Download](https://adoptium.net/)
- **Maven 3.8+** — [下载 / Download](https://maven.apache.org/download.cgi)
- **Git** — [下载 / Download](https://git-scm.com/)
- **OpenAI API Key**（或其他兼容的 LLM 提供商密钥）— [申请 / Get one](https://platform.openai.com/api-keys)
- **Ollama**（可选，用于本地模型示例 / Optional, for local model examples）— [下载 / Download](https://ollama.com/)

---

## 快速开始 / Quick Start

### 1. 克隆仓库 / Clone the Repository

```bash
git clone https://github.com/Refinex-Space/Refinex-SpringAI-Examples.git
cd Refinex-SpringAI-Examples
```

### 2. 配置 API 密钥 / Configure API Key

将您的 OpenAI API Key 设置为环境变量（推荐），或直接在对应模块的 `application.yml` / `application.properties` 中配置：

Set your OpenAI API key as an environment variable (recommended), or configure it directly in the module's `application.yml` / `application.properties`:

```bash
# Linux / macOS
export SPRING_AI_OPENAI_API_KEY=your-api-key-here

# Windows (CMD)
set SPRING_AI_OPENAI_API_KEY=your-api-key-here

# Windows (PowerShell)
$env:SPRING_AI_OPENAI_API_KEY="your-api-key-here"
```

### 3. 进入并运行示例模块 / Enter and Run an Example Module

```bash
# 以 chat-basic 为例 / Example: chat-basic
cd chat-basic
mvn spring-boot:run
```

---

## 配置说明 / Configuration

每个示例模块均包含一个 `src/main/resources/application.yml` 配置文件，主要配置项如下：

Each example module contains an `application.yml` configuration file. Key properties:

```yaml
spring:
  ai:
    openai:
      api-key: ${SPRING_AI_OPENAI_API_KEY}   # API 密钥 / API Key (use env var)
      base-url: https://api.openai.com       # API 地址 / API base URL
      chat:
        options:
          model: gpt-4o                      # 使用的模型 / Model to use
          temperature: 0.7                   # 创造性参数 / Creativity parameter
```

> ⚠️ **安全提示 / Security Notice**：请勿将 API 密钥直接硬编码到配置文件中，推荐使用环境变量或 Spring Boot 的 Secrets 管理机制。
>
> **Never hard-code your API key** in configuration files. Use environment variables or a secrets management solution.

---

## 项目结构 / Project Structure

```
Refinex-SpringAI-Examples/
├── chat-basic/                  # 基础对话示例 / Basic chat example
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   │       └── application.yml
│   └── pom.xml
├── chat-streaming/              # 流式对话示例 / Streaming chat example
├── rag-basic/                   # RAG 示例 / RAG example
├── ...                          # 其他模块 / Other modules
├── .gitignore
├── LICENSE
└── README.md
```

---

## 贡献指南 / Contributing

欢迎提交 Issue 和 Pull Request！在贡献代码之前，请确保：

Contributions via Issues and Pull Requests are welcome! Before contributing, please ensure:

1. **Fork** 本仓库并创建新分支 / Fork this repo and create a new branch
2. 遵循现有代码风格（Google Java Style）/ Follow the existing code style (Google Java Style)
3. 为新示例添加清晰的注释和 README / Add clear comments and README for new examples
4. 提交前运行 `mvn verify` 确保测试通过 / Run `mvn verify` before submitting to ensure tests pass
5. 提交 PR 时请详细描述改动内容 / Describe your changes clearly in the PR

---

## 许可证 / License

本项目基于 [MIT License](LICENSE) 开源。

This project is open-sourced under the [MIT License](LICENSE).

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/Refinex-Space">Refinex-Space</a>
</p>
