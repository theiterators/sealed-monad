import React from 'react';
import clsx from 'clsx';
import styles from './HomepageFeatures.module.css';

const FeatureList = [
  {
    title: 'Linear Code Flow',
    img: "img/easy_to_read_write.png",
    alt: "image of keyboard and monitor",
    description: (
      <>
        Write top-down, sequential code that's easy to follow. Focused on using for-comprehension syntax rather than composing combinators.
      </>
    )
  },
  {
    title: 'Business-Logic Focused',
    img: "img/flexible.png",
    alt: "image of cog wheel",
    description: (
      <>
        Error handling that focuses on representing business outcomes, not technical errors. Preserves locality and works with any monad.
      </>
    ),
  },
  {
    title: 'ADT-Driven Design',
    img: "img/driven.png",
    alt: "image of a wheel, wrench and a cogwheel",
    description: (
      <>
        Naturally works with sealed trait hierarchies to model operation results. Perfect for API responses with multiple possible outcomes.
      </>
    ),
  },
];


function Feature({img, alt, title, description}) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center">
        <img src={img} alt={alt}/>
      </div>
      <div className="text--center padding-horiz--md">
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
