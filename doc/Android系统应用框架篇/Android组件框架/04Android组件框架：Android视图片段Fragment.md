# Android组件框架：Android视图片段Fragment

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

**文章目录**

- Fragment管理流程
- Fragment生命周期
- Fragment回退栈

>A Fragment is a piece of an application's user interface or behavior that can be placed in an Activity.

Fragment放置在Activity容器中，通常用来作为UI的片段，在日常的开发中也有着广泛的应用，先来看一段常用的代码。

```java
DemoFragment demoFragment = DemoFragment.newInstance("param1", "param2");
Bundle bundle = new Bundle();
demoFragment.setArguments(bundle);
getSupportFragmentManager().beginTransaction()
        .add(R.id.fragment_container, demoFragment)
        .commit();
```

这是我们非常常见的代码，借着这段代码，引出我们今天的主题：针对Fragment的全面的源码分析。

## 一 Fragment操作方法

Fragment的操作是一种事务操作，什么是事务？🤔简单来说就是一个原子操作，要么被成功执行，否则原来的操作会回滚，各个操作彼此之间互不干扰。我们先整体看下Fragment的操作
序列图。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/fragment_operation_sequence.png" height="500"/>

嗯，看起来有点长😌，不要方，我们先来看看这里面频繁出现的几个类的作用。

- FragmentActivity：这个自不必说，它是Fragment的容器Activity，只有你的Activity继承自FragmentActivity，你才能使用Fragment，Android的AppCompatActivity就继承自FragmentActivity。
- FragmentManager：Fragment的管理是由FragmentManager这个类的完成的，我们通常在Activity中使用getSupportFragmentManager()方法来获取。它是一个抽象类，其实现类是FragmentManagerImpl。
- FragmentTransaction：定义了Fragment的所有操作，我们通常通过getSupportFragmentManager().beginTransaction()方法来获取。它是一个抽象类，其实现类是BackStackRecord，BackStackRecord将Fragment与相应应的
操作包装起来，传递给FragmentManager调用。
- FragmentHostCallback：抽象类，它将Fragment、Activity与FragmentManager串联成一个整体，FragmentActivity的内部类HostCallbacks继承了这个抽象类。
- FragmentController：它的主要职责是控制Fragment的生命周期，它在FragmentActivity里以HostCallbacks为参数被创建，持有HostCallbacks的引用。

### 1.1 操作的封装

Fragment的操作方法一共有七种：

- add
- replace
- remove
- hide
- show
- detach
- attach

```java
final class BackStackRecord extends FragmentTransaction implements
        FragmentManager.BackStackEntry, FragmentManagerImpl.OpGenerator {
    
        @Override
        public FragmentTransaction add(Fragment fragment, String tag) {
            doAddOp(0, fragment, tag, OP_ADD);
            return this;
        }
    
        @Override
        public FragmentTransaction add(int containerViewId, Fragment fragment) {
            doAddOp(containerViewId, fragment, null, OP_ADD);
            return this;
        }
    
        @Override
        public FragmentTransaction add(int containerViewId, Fragment fragment, String tag) {
            doAddOp(containerViewId, fragment, tag, OP_ADD);
            return this;
        }
        
         @Override
            public FragmentTransaction replace(int containerViewId, Fragment fragment) {
                return replace(containerViewId, fragment, null);
            }
        
            @Override
            public FragmentTransaction replace(int containerViewId, Fragment fragment, String tag) {
                if (containerViewId == 0) {
                    throw new IllegalArgumentException("Must use non-zero containerViewId");
                }
        
                doAddOp(containerViewId, fragment, tag, OP_REPLACE);
                return this;
            }
        
            @Override
            public FragmentTransaction remove(Fragment fragment) {
                Op op = new Op();
                op.cmd = OP_REMOVE;
                op.fragment = fragment;
                addOp(op);
        
                return this;
            }
        
            @Override
            public FragmentTransaction hide(Fragment fragment) {
                Op op = new Op();
                op.cmd = OP_HIDE;
                op.fragment = fragment;
                addOp(op);
        
                return this;
            }
        
            @Override
            public FragmentTransaction show(Fragment fragment) {
                Op op = new Op();
                op.cmd = OP_SHOW;
                op.fragment = fragment;
                addOp(op);
        
                return this;
            }
        
            @Override
            public FragmentTransaction detach(Fragment fragment) {
                Op op = new Op();
                op.cmd = OP_DETACH;
                op.fragment = fragment;
                addOp(op);
        
                return this;
            }
        
            @Override
            public FragmentTransaction attach(Fragment fragment) {
                Op op = new Op();
                op.cmd = OP_ATTACH;
                op.fragment = fragment;
                addOp(op);
        
                return this;
            }
}
```

你可以发现，这些方法最终都调用了addOp()方法，Op是什么？🤔Op封装了操作命令、Fragment、动画等内容。上面我们说过BackStackRecord将Fragment与相应应的操作包装起来，传递给FragmentManager调用。

```java
static final class Op {
    int cmd;
    Fragment fragment;
    int enterAnim;
    int exitAnim;
    int popEnterAnim;
    int popExitAnim;
}
```
cmd对应了响应的操作。

```java
static final int OP_NULL = 0;
static final int OP_ADD = 1;
static final int OP_REPLACE = 2;
static final int OP_REMOVE = 3;
static final int OP_HIDE = 4;
static final int OP_SHOW = 5;
static final int OP_DETACH = 6;
static final int OP_ATTACH = 7;
```

我们来看看addOp()方法的实现。

```java
final class BackStackRecord extends FragmentTransaction implements
        FragmentManager.BackStackEntry, FragmentManagerImpl.OpGenerator {
    
       ArrayList<Op> mOps = new ArrayList<>();
    
       void addOp(Op op) {
           mOps.add(op);
           op.enterAnim = mEnterAnim;
           op.exitAnim = mExitAnim;
           op.popEnterAnim = mPopEnterAnim;
           op.popExitAnim = mPopExitAnim;
       }
}
```

上面代码的最后一步是commit()方法，该方法提交事务操作，我们来看看它的实现。

```java
final class BackStackRecord extends FragmentTransaction implements
        FragmentManager.BackStackEntry, FragmentManagerImpl.OpGenerator {
    @Override
    public int commit() {
        return commitInternal(false);
    }
    
    //allowStateLoss是个标志位，表示是否允许状态丢失
    int commitInternal(boolean allowStateLoss) {
        if (mCommitted) throw new IllegalStateException("commit already called");
        if (FragmentManagerImpl.DEBUG) {
            Log.v(TAG, "Commit: " + this);
            LogWriter logw = new LogWriter(TAG);
            PrintWriter pw = new PrintWriter(logw);
            dump("  ", null, pw, null);
            pw.close();
        }
        mCommitted = true;
        if (mAddToBackStack) {
            mIndex = mManager.allocBackStackIndex(this);
        } else {
            mIndex = -1;
        }
        mManager.enqueueAction(this, allowStateLoss);
        return mIndex;
    }
}
```
可以看到BackStackRecord完成了对Fragment操作的封装，并比较给FragmentManager调用。

### 1.2 操作的调用

从上面的序列图我们可以看出，在commit()方法执行后，会调用FragmentManager.enqueueAction()方法，并通过handler.post()切换到主线程去执行这个Action，执行时间未知。
这个handler正是FragmentActivity里创建的Handler。


```java
final class BackStackRecord extends FragmentTransaction implements
        FragmentManager.BackStackEntry, FragmentManagerImpl.OpGenerator {
    
    void executeOps() {
        final int numOps = mOps.size();
        for (int opNum = 0; opNum < numOps; opNum++) {
            final Op op = mOps.get(opNum);
            final Fragment f = op.fragment;
            f.setNextTransition(mTransition, mTransitionStyle);
            //Fragment操作
            switch (op.cmd) {
                case OP_ADD:
                    f.setNextAnim(op.enterAnim);
                    mManager.addFragment(f, false);
                    break;
                case OP_REMOVE:
                    f.setNextAnim(op.exitAnim);
                    mManager.removeFragment(f);
                    break;
                case OP_HIDE:
                    f.setNextAnim(op.exitAnim);
                    mManager.hideFragment(f);
                    break;
                case OP_SHOW:
                    f.setNextAnim(op.enterAnim);
                    mManager.showFragment(f);
                    break;
                case OP_DETACH:
                    f.setNextAnim(op.exitAnim);
                    mManager.detachFragment(f);
                    break;
                case OP_ATTACH:
                    f.setNextAnim(op.enterAnim);
                    mManager.attachFragment(f);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown cmd: " + op.cmd);
            }
            if (!mAllowOptimization && op.cmd != OP_ADD) {
                mManager.moveFragmentToExpectedState(f);
            }
        }
        if (!mAllowOptimization) {
            // Added fragments are added at the end to comply with prior behavior.
            mManager.moveToState(mManager.mCurState, true);
        }
    }
}
```

因而，Fragment的操作：

- add
- remove
- replace
- hide
- show
- detach
- attach

都转换成了FragmentManager的方法：

- addFragment
- removeFragment
- removeFragment + addFragment
- hideFragment
- showFragment
- detachFragment
- attachFragment

并调用FragmentManager.moveToState()方法做Fragment的状态迁移。上述的这几种Fragment的操作方法都做了哪些事情呢？🤔

- 将Fragment从mAdded列表中添加或移除。
- 改变Fragment的mAdded、mRemoving、mHidden等标志位

要理解以下方法，我们要先看看Fragment里的几个标志位的含义。

- boolean mAdded：表示Fragment是否被添加到FragmentManager里的Fragment列表mAdded中。
- boolean mRemoving：表示Fragment是否从Activity中移除。
- boolean mHidden：表示Fragment是否对用户隐藏。
- boolean mDetached：表示Fragment是否已经从宿主Activity中分离。

```java
final class FragmentManagerImpl extends FragmentManager implements LayoutInflaterFactory {
    
    //添加Fragment
   public void addFragment(Fragment fragment, boolean moveToStateNow) {
          if (mAdded == null) {
              mAdded = new ArrayList<Fragment>();
          }
          if (DEBUG) Log.v(TAG, "add: " + fragment);
          makeActive(fragment);
          if (!fragment.mDetached) {
              if (mAdded.contains(fragment)) {
                  throw new IllegalStateException("Fragment already added: " + fragment);
              }
              synchronized (mAdded) {
                  mAdded.add(fragment);
              }
              fragment.mAdded = true;
              fragment.mRemoving = false;
              if (fragment.mView == null) {
                  fragment.mHiddenChanged = false;
              }
              if (fragment.mHasMenu && fragment.mMenuVisible) {
                  mNeedMenuInvalidate = true;
              }
              if (moveToStateNow) {
                  moveToState(fragment);
              }
          }
      }
  
      //移除Fragment
      public void removeFragment(Fragment fragment) {
          if (DEBUG) Log.v(TAG, "remove: " + fragment + " nesting=" + fragment.mBackStackNesting);
          final boolean inactive = !fragment.isInBackStack();
          if (!fragment.mDetached || inactive) {
              if (mAdded != null) {
                  synchronized (mAdded) {
                      mAdded.remove(fragment);
                  }
              }
              if (fragment.mHasMenu && fragment.mMenuVisible) {
                  mNeedMenuInvalidate = true;
              }
              fragment.mAdded = false;
              fragment.mRemoving = true;
          }
      }
      
      //隐藏Fragment：将一个Fragment标记成将要隐藏状态，显示工作有completeShowHideFragment(}方法完成
      public void hideFragment(Fragment fragment) {
          if (DEBUG) Log.v(TAG, "hide: " + fragment);
          if (!fragment.mHidden) {
              fragment.mHidden = true;
              // Toggle hidden changed so that if a fragment goes through show/hide/show
              // it doesn't go through the animation.
              fragment.mHiddenChanged = !fragment.mHiddenChanged;
          }
      }
  
      //显示Fragment：将一个Fragment标记成将要显示状态，显示工作有completeShowHideFragment(}方法完成
      public void showFragment(Fragment fragment) {
          if (DEBUG) Log.v(TAG, "show: " + fragment);
          if (fragment.mHidden) {
              fragment.mHidden = false;
              // Toggle hidden changed so that if a fragment goes through show/hide/show
              // it doesn't go through the animation.
              fragment.mHiddenChanged = !fragment.mHiddenChanged;
          }
      }
  
      //将Fragment从宿主Activity分离
      public void detachFragment(Fragment fragment) {
          if (DEBUG) Log.v(TAG, "detach: " + fragment);
          if (!fragment.mDetached) {
              fragment.mDetached = true;
              if (fragment.mAdded) {
                  // We are not already in back stack, so need to remove the fragment.
                  if (mAdded != null) {
                      if (DEBUG) Log.v(TAG, "remove from detach: " + fragment);
                      synchronized (mAdded) {
                          mAdded.remove(fragment);
                      }
                  }
                  if (fragment.mHasMenu && fragment.mMenuVisible) {
                      mNeedMenuInvalidate = true;
                  }
                  fragment.mAdded = false;
              }
          }
      }
  
      //将Fragment关联3到宿主Activity
      public void attachFragment(Fragment fragment) {
          if (DEBUG) Log.v(TAG, "attach: " + fragment);
          if (fragment.mDetached) {
              fragment.mDetached = false;
              if (!fragment.mAdded) {
                  if (mAdded == null) {
                      mAdded = new ArrayList<Fragment>();
                  }
                  if (mAdded.contains(fragment)) {
                      throw new IllegalStateException("Fragment already added: " + fragment);
                  }
                  if (DEBUG) Log.v(TAG, "add from attach: " + fragment);
                  synchronized (mAdded) {
                      mAdded.add(fragment);
                  }
                  fragment.mAdded = true;
                  if (fragment.mHasMenu && fragment.mMenuVisible) {
                      mNeedMenuInvalidate = true;
                  }
              }
          }
      }
}
```

可以看到这些方法大体类似，差别在于它们处理的标志位不同，这也导致了后续的moveToState()在处理它们的时候回区别对待，具体说来：

- add操作添加一个Fragment，会依次调用 onAttach, onCreate, onCreateView, onStart and onResume 等方法。
- attach操作关联一个Fragment，会依次调用onCreateView, onStart and onResume 。
- remove操作移除一个Fragment，会依次调用nPause, onStop, onDestroyView, onDestroy and onDetach 等方法。
- detach操作分离一个Fragment，会依次调用onPause, onStop and onDestroyView  等方法。

detach后的Fragment可以再attach，而remove后的Fragment却不可以，只能重新add。

理解完了Fragment的操作，我们再来看看它的生命周期的变化，这也是我们的重点。

## Fragment生命周期

我们先来看一张完整的Fragment生命周期图。




我们都知道Fragment的生命周期依赖于它的宿主Activity，但事实的情况却并不这么简单。

- onAttach：当Fragment与宿主Activity建立联系的时候调用。
- onCreate：用来完成Fragment的初始化创建工作。
- onCreateView：创建并返回View给Fragment。
- onActivityCreated：通知Fragment当前Activity的onCreate()方法已经调用完成。
- onViewStateRestored：通知Fragment以前保存的View状态都已经被恢复。
- onStart：Fragment已经对用户可见时调用，当然这个基于它的宿主Activity的onStart()方法已经被调用。
- onResume：Fragment已经开始和用户交互时调用，当然这个基于它的宿主Activity的onResume()方法已经被调用。
- onPause：Fragment不再和用户交互时调用，这通常发生在宿主Activity的onPause()方法被调用或者Fragment被修改（replace、remove）。
- onStop：Fragment不再对用户可见时调用，这通常发生在宿主Activity的onStop()方法被调用或者Fragment被修改（replace、remove）。
- onDestroyView：Fragment释放View资源时调用。
- onDetach：Fragment与宿主Activity脱离联系时调用。

